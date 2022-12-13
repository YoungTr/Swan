//
// Created by YoungTr on 2022/12/13.
//

#ifndef SWAN_SCOPEDCLEANER_H
#define SWAN_SCOPEDCLEANER_H

#include <utility>


namespace swan {
    template<class TDtor>
    class ScopedCleaner {
    public:
        ScopedCleaner(const TDtor &dtor) : m_TDtor_(dtor), m_Omitted_(false) {};

        ScopedCleaner(ScopedCleaner &&other) : m_TDtor_(other.m_TDtor_),
                                               m_Omitted_(other.m_Omitted_) {
            other.Omit();
        }

        void Omit() {
            m_Omitted_ = true;
        }

    private:
        void *operator new(size_t) = delete;

        void *operator new[](size_t) = delete;

        ScopedCleaner(const ScopedCleaner &) = delete;

        ScopedCleaner &operator=(const ScopedCleaner &) = delete;

        ScopedCleaner &operator=(ScopedCleaner &&) = delete;

        TDtor m_TDtor_;
        bool m_Omitted_;
    };

    template<class TDtor>
    static ScopedCleaner<TDtor> MakeScopedCleaner(const TDtor &dtor) {
        return ScopedCleaner<TDtor>(std::forward<const TDtor &>(dtor));
    }

}

#endif //SWAN_SCOPEDCLEANER_H
