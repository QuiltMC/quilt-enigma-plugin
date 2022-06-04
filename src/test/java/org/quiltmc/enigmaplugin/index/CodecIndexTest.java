/*
 * Copyright 2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.enigmaplugin.index;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class CodecIndexTest {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: CodecIndexTest <path> [<customCodec>...]");
            System.exit(1);
        }

        Path path = Path.of(args[0]);
        ClassNode node = new ClassNode();
        try {
            byte[] classFile = Files.readAllBytes(path);
            ClassReader reader = new ClassReader(classFile);
            reader.accept(node, 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read class file", e);
        }

        CodecIndex index = new CodecIndex();
        if (args.length > 1) {
            List<String> customCodecs = Arrays.asList(args).subList(1, args.length);
            index.addCustomCodecs(customCodecs);
        }

        index.visitClassNode(node);

        dumpIndex(index);
    }

    private static void dumpIndex(CodecIndex index) {
        System.out.println("CodecIndex");
        if (index.hasCustomCodecs()) {
            System.out.println("  Custom codecs:");
            for (String codec : index.getCustomCodecs()) {
                System.out.println("    " + codec);
            }
        }

        System.out.println("\nFields:\n");
        if (index.getFieldNames().isEmpty()) {
            System.out.println("  No fields");
        } else {
            index.getFieldNames().forEach(((field, s) -> System.out.println(field + " " + s)));
        }
        System.out.println("\nMethods:\n");
        if (index.getMethodNames().isEmpty()) {
            System.out.println("  No methods");
        } else {
            index.getMethodNames().forEach(((method, s) -> System.out.println(method + " " + s)));
        }
    }
}
