package allpackage;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("请输入命令（huff/unhuff/preview/exit）及相应的路径：");
            String commandLine = scanner.nextLine();
            String[] commandArgs = commandLine.split(" ");

            if (commandArgs[0].equalsIgnoreCase("exit")) {
                System.out.println("程序已退出。");
                break;
            }

            processCommand(commandArgs);
        }
    }

    private static void processCommand(String[] args) {
        if (args.length < 2) {
            System.out.println("参数不足，请重新输入。");
            return;
        }

        String command = args[0];
        String inputPathName = args[1];

        switch (command.toLowerCase()) {
            case "huff":
                Compression(args, inputPathName);
                break;
            case "unhuff":
                Decompression(inputPathName);
                break;
            case "preview":
                FolderDecompression.previewCompressedStructure(inputPathName);
                break;
            default:
                System.out.println("未知命令，请使用 'huff', 'unhuff', 'preview' 或 'exit'");
        }
    }

    private static void Compression(String[] args, String inputPathName) {
        if (args.length < 3) {
            System.out.println("参数不完整，请输入outputpath。");
            return;
        }

        long startTime = System.currentTimeMillis(); // 获取开始时间
        String outputPathName = args[2];
        File inputFile = new File(inputPathName);

        if (inputFile.isDirectory()) {
            // 文件夹压缩
            FolderCompression.finalFolderCompression(inputPathName, outputPathName);
        } else {
            // 文件压缩
            HuffmanCompression.finalHuffmanCompression(inputPathName, outputPathName);
        }

        long endTime = System.currentTimeMillis(); // 获取结束时间
        displayCompressionDetails(inputFile, outputPathName, startTime, endTime, true);
    }

    private static void Decompression(String inputPathName) {
        long startTime = System.currentTimeMillis(); // 获取开始时间

        File inputFile = new File(inputPathName);

        if (!inputFile.exists()) {
            System.out.println("压缩文件不存在。");
            return;
        }

        try (DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(inputPathName)))) {
            byte[] magicNumber = new byte[5];
            dataInputStream.readFully(magicNumber);

            if (Arrays.equals(magicNumber, HuffmanCompression.FILE_MAGIC_NUMBER)) {
                HuffmanDecompression.finalHuffmanDecompression(inputPathName);
            } else if (Arrays.equals(magicNumber, HuffmanCompression.FOLDER_MAGIC_NUMBER)) {
                FolderDecompression.finalFolderDecompression(inputPathName);
            } else {
                System.out.println("这不是我创建的文件，无法解压!");
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        long endTime = System.currentTimeMillis(); // 获取结束时间
        displayCompressionDetails(new File(inputPathName), null, startTime, endTime, false);
    }

    private static void displayCompressionDetails(File inputFile, String outputPath, long startTime, long endTime, boolean isCompression) {
        // 计算耗时并转换为毫秒
        double durationMillis = (double)(endTime - startTime); // 确保转换为double

        // 将毫秒转换为秒
        double durationSeconds = durationMillis / 1000.0; // 使用1000.0以确保结果是double

        System.out.println("操作完成！");
        System.out.println("耗时: " + String.format("%.3f", durationSeconds) + " 秒");


        // 仅在压缩时显示额外的信息
        if (isCompression) {
            long inputSize = inputFile.isDirectory() ? calculateFolderSize(inputFile) : inputFile.length();
            File outputFile = outputPath != null ? new File(outputPath) : inputFile;
            long outputSize = outputFile.length();

            // 转换为KB，并保留两位小数
            double inputSizeKB = inputSize / 1024.0;
            double outputSizeKB = outputSize / 1024.0;

            // 压缩率计算，转换为百分比
            double compressionRatio = inputSize != 0 ? ((double) outputSize / inputSize) * 100.0 : 0;

            System.out.println("原始大小: " + String.format("%.2f", inputSizeKB) + " KB");
            System.out.println("输出大小: " + String.format("%.2f", outputSizeKB) + " KB");
            System.out.println("压缩率: " + String.format("%.2f", compressionRatio) + "%");
        }
    }

    private static long calculateFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                length += file.isDirectory() ? calculateFolderSize(file) : file.length();
            }
        }

        return length;
    }



}
