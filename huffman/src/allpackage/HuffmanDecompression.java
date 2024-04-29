package allpackage;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import allpackage.HuffmanCompression.Node;
public class HuffmanDecompression {

    //根据存入压缩文件的哈夫曼编码表重建哈夫曼树
    public static Node rebuildHuffmanTree(HashMap<Byte, String> huffmanCodes) {
        Node root = new Node((byte) 0, 0);
        for (Map.Entry<Byte, String> entry : huffmanCodes.entrySet()) {
            byte character = entry.getKey();
            String code = entry.getValue();
            HuffmanCompression.Node current = root;

            for (int i = 0; i < code.length(); i++) {
                if (code.charAt(i) == '0') {
                    if (current.left == null) {
                        current.left = new Node((byte) 0, 0);
                    }
                    current = current.left;
                } else {
                    if (current.right == null) {
                        current.right = new Node((byte) 0, 0);
                    }
                    current = current.right;
                }
            }
            current.data = character;
        }
        return root;
    }

    private static boolean isLeaf(Node root) {
        return root != null && root.left == null && root.right == null;
    }

    //从压缩文件中读取哈夫曼编码表
    public static HashMap<Byte, String> readHuffmanCodesFromStream(DataInputStream dataInputStream) {
        HashMap<Byte, String> huffmanCodes = new HashMap<>();
        try {
            int codesCount = dataInputStream.readInt(); // 读取编码表的大小

            for (int i = 0; i < codesCount; i++) {
                byte character = dataInputStream.readByte(); // 读取字符
                int codeLength = dataInputStream.readInt(); // 读取编码长度
                StringBuilder codeBuilder = new StringBuilder(codeLength);

                for (int j = 0; j < codeLength; j++) {
                    char c = dataInputStream.readChar(); // 读取编码字符
                    codeBuilder.append(c);
                }

                huffmanCodes.put(character, codeBuilder.toString()); // 将字符和对应的编码添加到哈夫曼编码表中
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return huffmanCodes;
    }


    //通过哈夫曼树对二进制数据进行解压缩
    public static void decompressBinaryData(BufferedOutputStream bufferedOutputStream, Node root, byte[] encodedData) throws IOException {
        if (root == null) {
            System.err.println("Warning: Huffman tree root is null. Cannot decompress data.");
            return;
        }
            Node current = root;
            for (byte data : encodedData) {
                for (int bit = 7; bit >= 0; bit--) {
                    Node child = (data & (1 << bit)) == 0 ? current.left : current.right;

                    if (isLeaf(child)) {
                        bufferedOutputStream.write(child.data);
                        current = root;
                    } else {
                        current = child;
                    }
                }
            }

    }


    //解压缩
    public static void decompressFile(String inputFilePath) {
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
            String originalFileName = dataInputStream.readUTF();

            // 构建解压缩后的输出路径，放在压缩文件的同一目录下
            String outputDirectory = new File(inputFilePath).getParent();
            String outputFilePath = Paths.get(outputDirectory, originalFileName).toString();

            // 读取哈夫曼编码表
            HashMap<Byte, String> huffmanCodes = readHuffmanCodesFromStream(dataInputStream);

            // 读取哈夫曼编码后的二进制数据
            ByteArrayOutputStream encodedDataBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                encodedDataBuffer.write(buffer, 0, bytesRead);
            }
            byte[] encodedData = encodedDataBuffer.toByteArray();

            // 重构哈夫曼树
            Node root = rebuildHuffmanTree(huffmanCodes);

            // 解码并写入文件
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(outputFilePath))) {
                decompressBinaryData(bufferedOutputStream, root, encodedData);
            }

            System.out.println("解压缩完成：" + outputFilePath);

            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //考虑多种异常情况并加入交互
    public static void finalHuffmanDecompression(String inputFilePath) {
        File inputFile = new File(inputFilePath);

        // 从压缩文件中获取原始文件名
        String originalFileName;
        try {
            originalFileName = getOriginalFileName(inputFilePath);
        } catch (IOException e) {
            System.out.println("读取原始文件名时发生错误: " + e.getMessage());
            return;
        }

        // 检查是否存在同名文件
        File outputFile = new File(inputFile.getParent(), originalFileName);
        if (outputFile.exists()) {
            System.out.print("文件夹中存在同名文件，是否覆盖？(输入 y 或 n): ");
            Scanner scanner = new Scanner(System.in);
            String userChoice = scanner.nextLine();

            if (!userChoice.equalsIgnoreCase("y")) {
                System.out.println("操作已取消。");
                return;
            }
        }

        System.out.println("正在进行解压缩...");

        decompressFile(inputFilePath); // 假设这个方法处理了解压缩逻辑
        System.out.println("解压缩完毕！");
    }


    //获取原文件名以判断是否输出目录下有同名文件
    private static String getOriginalFileName(String compressedFilePath) throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(compressedFilePath))) {
            // 跳过 magic number
            byte[] magicNumber = new byte[FILE_MAGIC_NUMBER.length];
            dataInputStream.readFully(magicNumber);

            // 检查 magic number 是否匹配
            if (!Arrays.equals(magicNumber, FILE_MAGIC_NUMBER)) {
                throw new IOException("这不是我创建的文件，无法解压!");
            }

            // 读取并返回原始文件名
            return dataInputStream.readUTF();
        }
    }

    public static final byte[] FILE_MAGIC_NUMBER = {0x48, 0x46, 0x49, 0x4C, 0x45}; // "HFILE"
    public static boolean checkMagicNumber(DataInputStream dataInputStream) throws IOException {
        byte[] actualMagicNumber = new byte[FILE_MAGIC_NUMBER.length];
        dataInputStream.readFully(actualMagicNumber);
        // 比较读取到的 Magic Number 与预期的 Magic Number 是否一致
        return Arrays.equals(actualMagicNumber, FILE_MAGIC_NUMBER);
    }




}
