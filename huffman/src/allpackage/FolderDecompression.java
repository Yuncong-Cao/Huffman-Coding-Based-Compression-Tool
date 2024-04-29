package allpackage;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;

import static allpackage.HuffmanDecompression.*;

public class FolderDecompression {

    public static void decompressFolder(String inputFilePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(inputFilePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

            // 读取 Magic Number
            if (!checkMagicNumber(dataInputStream)) {
                System.out.println("这不是我创建的文件，无法解压!");
                return;
            }

            // 读取文件名
            String originalFolderName = dataInputStream.readUTF();

            // 构建解压缩后的文件夹路径，放在压缩文件的同一目录下
            String outputFolderPath = Paths.get(new File(inputFilePath).getParent(), originalFolderName).toString();
            File outputFolder = new File(outputFolderPath);

            // 解压缩文件夹
            decompressFolderRecursive(dataInputStream, outputFolder);

            System.out.println("解压缩完成：" + outputFolderPath);

            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decompressFolderRecursive(DataInputStream dataInputStream, File outputFolder) throws IOException {

        while (dataInputStream.available() > 0) {
            String type = dataInputStream.readUTF(); // 读取类型标识符
            String name = dataInputStream.readUTF(); // 读取文件名或文件夹名
            File currentFile = new File(outputFolder, name);

            if (type.equals("F")) {
                // 如果是文件夹
                currentFile.mkdirs();
            } else if (type.equals("FI")) {
                // 如果是文件
                decompressFile(dataInputStream, currentFile);
            }
        }
    }

    private static void decompressFile(DataInputStream dataInputStream, File outputFile) throws IOException {
        // 读取文件数据长度
        int fileLength = dataInputStream.readInt();

        // 读入最后一个字节中有效位
        int effectiveBitsLastByte = dataInputStream.readByte();

        // 读取哈夫曼编码表
        HashMap<Byte, String> huffmanCodes = readHuffmanCodesFromStream(dataInputStream);

        // 读取指定长度的哈夫曼编码后的二进制数据
        byte[] encodedData = new byte[fileLength];
        dataInputStream.readFully(encodedData);

        // 重构哈夫曼树
        HuffmanCompression.Node root = rebuildHuffmanTree(huffmanCodes);

        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 解码并写入文件
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            decompressBinaryData(bufferedOutputStream, root, encodedData, effectiveBitsLastByte);
        }
    }

    public static void decompressBinaryData(BufferedOutputStream bufferedOutputStream, HuffmanCompression.Node root, byte[] encodedData, int effectiveBitsLastByte) throws IOException {
        if (root == null) {
            System.err.println("Warning: Huffman tree root is null. Cannot decompress data.");
            return;
        }

        HuffmanCompression.Node current = root;
        for (int i = 0; i < encodedData.length; i++) {
            byte data = encodedData[i];
            int bits = (i == encodedData.length - 1) ? effectiveBitsLastByte : 8;  // Use effectiveBitsLastByte for the last byte

            for (int bit = bits - 1; bit >= 0; bit--) {
                HuffmanCompression.Node child = (data & (1 << bit)) == 0 ? current.left : current.right;

                if (HuffmanCompression.isLeaf(child)) {
                    bufferedOutputStream.write(child.data);
                    current = root;
                } else {
                    current = child;
                }
            }
        }
    }


    //返回文件夹名以判断是否已经有同名文件夹
    private static String readDecompressedFolderName(String inputFilePath) throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(inputFilePath));
             DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
        ) {
            // 读取 magic number，确保与预期相符
            byte[] magicNumber = new byte[FOLDER_MAGIC_NUMBER.length];
            dataInputStream.readFully(magicNumber);

            // 读取并返回文件夹名
            return dataInputStream.readUTF();
        }
        catch (IOException e) {
            System.out.println("获取文件夹名过程中发生错误: " + e.getMessage());
            return ""; // 返回空字符串或其他默认值
        }

    }

    public static final byte[] FOLDER_MAGIC_NUMBER = {0x48, 0x46, 0x4F, 0x4C, 0x44}; // "HFOLD"
    public static boolean checkMagicNumber(DataInputStream dataInputStream) throws IOException {
        byte[] actualMagicNumber = new byte[FOLDER_MAGIC_NUMBER.length];
        dataInputStream.readFully(actualMagicNumber);
        // 比较读取到的 Magic Number 与预期的 Magic Number 是否一致
        return Arrays.equals(actualMagicNumber, FOLDER_MAGIC_NUMBER);
    }


    //考虑多种异常情况并加入交互
    public static void finalFolderDecompression(String inputFilePath) {
        File inputFile = new File(inputFilePath);

        if (!inputFile.exists()) {
            System.out.println("压缩文件不存在。");
            return;
        }

        String decompressedFolderName;
        try {
            decompressedFolderName = readDecompressedFolderName(inputFilePath);
        } catch (IOException e) {
            System.out.println("读取解压缩文件夹名称时发生错误: " + e.getMessage());
            return;
        }

        File decompressedFolder = new File(inputFile.getParent(), decompressedFolderName);

        if (decompressedFolder.exists()) {
            System.out.print("同名文件夹已存在。是否覆盖？(输入 y 或 n): ");
            Scanner scanner = new Scanner(System.in);
            String userChoice = scanner.nextLine();

            if (userChoice.equalsIgnoreCase("n")) {
                System.out.println("操作已取消。");
                return;
            } else if (!userChoice.equalsIgnoreCase("y")) {
                System.out.println("无效输入。操作已取消。");
                return;
            }
        }

        System.out.println("正在进行解压缩...");
        decompressFolder(inputFilePath); // Assume this method handles decompression based on inputFilePath
        System.out.println("解压缩完毕！");
    }




    //实现预览
    public static void previewCompressedStructure(String inputFilePath) {
        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);) {

            // 读取 Magic Number
            if (!checkMagicNumber(dataInputStream)) {
                System.out.println("这不是我创建的文件夹压缩文件，无法解压!");
                return;
            }

            System.out.print(readDecompressedFolderName(inputFilePath));
            System.out.print("\n");

            // 读取并忽略主文件夹名称
            dataInputStream.readUTF();

            Stack<String> pathStack = new Stack<>();
            printFolderStructure(dataInputStream, pathStack, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printFolderStructure(DataInputStream dataInputStream, Stack<String> pathStack, int level) throws IOException {
        while (dataInputStream.available() > 0) {
            String type = dataInputStream.readUTF();
            String path = dataInputStream.readUTF();

            // 调整栈深度
            level = adjustLevel(pathStack, path, level);

            if (type.equals("F")) {
                printIndentedName(path, level);
                pathStack.push(path);
                printFolderStructure(dataInputStream, pathStack, level + 1);
            } else if (type.equals("FI")) {
                printIndentedName(path, level);
                skipCompressedFileData(dataInputStream);
            }
        }
    }

    private static int adjustLevel(Stack<String> pathStack, String currentPath, int currentLevel) {
        while (!pathStack.isEmpty() && !currentPath.startsWith(pathStack.peek() + File.separator)) {
            pathStack.pop();
            currentLevel--;
        }
        return currentLevel;
    }

    private static void printIndentedName(String path, int level) {
        for (int i = 0; i < level; i++) {
            System.out.print("│   ");
        }
        String name = path.substring(path.lastIndexOf(File.separator) + 1);
        System.out.println("├── " + name);
    }

    private static void skipCompressedFileData(DataInputStream dataInputStream) throws IOException {
        int fileLength = dataInputStream.readInt();
        dataInputStream.readByte(); // 跳过最后一个字节中有效位
        skipHuffmanCodes(dataInputStream);
        dataInputStream.skipBytes(fileLength); // 跳过压缩数据
    }

    private static void skipHuffmanCodes(DataInputStream dataInputStream) throws IOException {
        int codesCount = dataInputStream.readInt();
        for (int i = 0; i < codesCount; i++) {
            dataInputStream.readByte(); // 跳过字符
            int codeLength = dataInputStream.readInt();
            dataInputStream.skipBytes(codeLength * 2); // 跳过编码
        }
    }




}
