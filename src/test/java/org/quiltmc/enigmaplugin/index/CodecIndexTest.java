package org.quiltmc.enigmaplugin.index;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.IOException;
import java.io.PrintWriter;
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
        // node.accept(new TraceClassVisitor(new PrintWriter(System.out)));
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
