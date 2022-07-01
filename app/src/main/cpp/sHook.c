//
// Created by YoungTr on 2022/6/28.
//

#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include <EGL/egl.h>
#include <GLES/gl.h>
#include <elf.h>
#include <fcntl.h>
#include <sys/mman.h>
#include <jni.h>

#include <android/log.h>
#include <inttypes.h>
#include <dlfcn.h>
#include "sHook.h"
#include "log.h"

#define PAGE_START(address) ((address) & PAGE_MASK)
#define PAGE_END(address)   (PAGE_START(address) + PAGE_SIZE)

size_t (*original_fwrite)(const void *buf, size_t size, size_t count, FILE *fp);

static void *get_module_base_addr(pid_t pid, const char *module_name) {
    FILE *fp;
    uintptr_t addr = 0;
    char file_name[32] = "\n";
    char line[1024] = "\n";
    if (pid < 0) {
        snprintf(file_name, sizeof(file_name), "/proc/self/maps");
    } else {
        snprintf(file_name, sizeof(file_name), "/proc/%d/maps", pid);
    }

    // 获取指定 pid 进程加载的内存模块信息
    fp = fopen(file_name, "r");
    while (fgets(line, sizeof(line), fp)) {
        if (NULL != strstr(line, module_name)
            && 1 == sscanf(line, "%"PRIxPTR"-%*lx %*4s 00000000", &addr))
            break;
    }
    fclose(fp);
    return (void *) addr;


}

size_t new_fwrite(const void *buf, size_t size, size_t count, FILE *fp) {
    //这里插入一段文本
    const char *text = "hello ";
    LOGD("hook fwrite success insert text: %s", text);
    original_fwrite(text, strlen(text), 1, fp);
    return original_fwrite(buf, size, count, fp);
}

int hook_fwrite(const char *so_path) {
    LOGD("so path: %s", so_path);
    // 1. 获取目标进程中模块的加载地址
    void *base_addr = get_module_base_addr(gettid(), so_path);
    LOGD("base addr = 0x%08X", base_addr);
    // 2. 保存被hook的目标函数的原始调用地址
    original_fwrite = fwrite;
    LOGD("original fwrite = 0x%08X", original_fwrite);
    // 3. 计算 PHT 实际地址
    Elf32_Ehdr *header = (Elf32_Ehdr *) base_addr;
    LOGD("e_ident = %d", header->e_ident[0]);
    if (0 != memcmp(header->e_ident, "\177ELF", 4)) {
        LOGD("memcmp false");
        return -1;
    }

    // //基址 + Elf32_Off e_phoff_PROGRAM_HEADER_OFFSET_IN_FILE(e_phoff)的值 = program_header_table的地址
    Elf32_Phdr *phdr_table = (Elf32_Phdr *) (base_addr + header->e_phoff);
    if (0 == phdr_table) {
        LOGD("phdr_table address: 0");
        return -1;
    }
    size_t phdr_count = header->e_phnum;
    LOGD("phdr count: %d", phdr_count);
    unsigned long p_vaddr = 0;
    unsigned int p_memsz = 0;
    for (int j = 0; j < phdr_count; ++j) {
        if (PT_DYNAMIC == phdr_table[j].p_type) {
            p_vaddr = phdr_table[j].p_vaddr + base_addr;
            p_memsz = phdr_table[j].p_memsz;
            break;
        }
    }

    LOGD("p_vaddr: %x", p_vaddr);
    LOGD("p_memsz: %x", p_memsz);

    Elf32_Dyn *dynamic_table = (Elf32_Dyn *) p_vaddr;
    unsigned long jmpRelOff = 0;
    unsigned long strTabOff = 0;
    unsigned long pltRelSz = 0;
    unsigned long symTabOff = 0;
    int dyn_count = p_memsz / sizeof(Elf32_Dyn);

    for (int i = 0; i < dyn_count; ++i) {
        int val = dynamic_table[i].d_un.d_val;
        switch (dynamic_table[i].d_tag) {
            case DT_JMPREL:
                jmpRelOff = val;
                break;
            case DT_STRTAB:
                strTabOff = val;
                break;
            case DT_PLTRELSZ:
                pltRelSz = val / sizeof(Elf32_Rel);
                break;
            case DT_SYMTAB:
                symTabOff = val;
                break;
        }
    }

    Elf32_Rel *rel_table = (Elf32_Rel *) (jmpRelOff + base_addr);
    LOGD("jmpRelOff : %x", jmpRelOff);
    LOGD("strTabOff : %x", strTabOff);
    LOGD("symTabOff : %x", symTabOff);

    // 遍历查找要 hook 的导入函数
    for (int i = 0; i < pltRelSz; ++i) {
        uint16_t ndx = ELF32_R_SYM(rel_table[i].r_info);
        Elf32_Sym *symTableIndex = (Elf32_Sym *) (ndx * sizeof(Elf32_Sym) + symTabOff + base_addr);
        char *func_name = (char *) (symTableIndex->st_name + strTabOff + base_addr);
        if (0 == memcmp(func_name, "fwrite", strlen("fwrite"))) {
            // 获取当前内存分页的大小
            uint32_t page_size = getpagesize();
            //获取内存分页的起始地址(需要内存对齐)
            uint32_t mem_page_start = rel_table[i].r_offset + base_addr;
            LOGD("old_function=0x%08X new_function=0x%08X", mem_page_start, new_fwrite);
            LOGD("mem_page_start=0x%08X, page size=%d", mem_page_start, page_size);
            mprotect((uint32_t) PAGE_START(mem_page_start), page_size,
                     PROT_READ | PROT_WRITE | PROT_EXEC);
            //完成替换操作
            *(unsigned int *) (rel_table[i].r_offset + base_addr) = new_fwrite;
            //清除指令缓存
            __builtin___clear_cache((void *) PAGE_START(mem_page_start),
                                    (void *) PAGE_END(mem_page_start));
        }
    }

    return 0;

}