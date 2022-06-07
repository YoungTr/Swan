package shark.internal;

import okio.ByteString;

public class ByteStringCompat {


  public static ByteString encodeUtf8(String string) {
    return ByteString.encodeUtf8(string);
  }
}
