package com.kingpixel.cobbleutils.util;

import net.minecraft.nbt.*;

public class NbtUtils {
  public static Object convertNbtValue(NbtElement element) {
    return switch (element) {
      case null -> null;
      case NbtByte byteTag -> {
        byte b = byteTag.byteValue();
        if (b == 0 || b == 1) yield b == 1;
        yield b;
      }
      case NbtShort shortTag -> shortTag.shortValue();
      case NbtInt intTag -> intTag.intValue();
      case NbtLong longTag -> longTag.longValue();
      case NbtFloat floatTag -> floatTag.floatValue();
      case NbtDouble doubleTag -> doubleTag.doubleValue();
      case NbtString stringTag -> stringTag.asString();
      default -> null;
    };
  }
}
