package allpackage;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

import static allpackage.HuffmanCompression.*;

public class FolderCompression {

    //压缩文件夹
    public static void compressFolder(String inputFolderPath, String outputFilePath) {
        try {
            // 检查输出文件路径是否为null，如果是，设置为默认路径
            if (outputFilePath == null || outputFilePath.isEmpty()) {
                outputFilePath = inputFolderPath + ".huff"; // 默认扩展名为 .huff
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

            // 写入 magic number
            dataOutputStream.write(FOLDER_MAGIC_NUMBER);

            // 获取并写入主文件夹名称
            File inputFolder = new File(inputFolderPath);
            String folderName = inputFolder.getName();
            dataOutputStream.writeUTF(folderName);

            // 压缩文件夹
            compressFolderRecursive(new File(inputFolderPath), "", dataOutputStream);

            // 关闭流
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //递归压缩的具体过程
    private static void compressFolderRecursive(File folder, String relativePath, DataOutputStream dataOutputStream) throws IOException {

        // 列出文件夹中的所有文件和子文件夹
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 写入文件夹标识符和相对路径
                    dataOutputStream.writeUTF("F");
                    dataOutputStream.writeUTF(relativePath + file.getName());
                    // 递归处理子文件夹，传递相对路径
                    compressFolderRecursive(file, relativePath + file.getName() + File.separator, dataOutputStream);
                } else {
                    // 写入文件标识符和相对路径
                    dataOutputStream.writeUTF("FI");
                    dataOutputStream.writeUTF(relativePath + file.getName());
                    // 处理文件
                    compressFile(file, dataOutputStream);
                }
            }
        }
    }

    //压缩单个文件
    private static void compressFile(File file, DataOutputStream dataOutputStream) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

            // 构建哈夫曼树
            bufferedInputStream.mark(Integer.MAX_VALUE);
            Node root = buildHuffmanTree(convertInputStreamToByteArray(bufferedInputStream));

            // 生成哈夫曼编码表
            HashMap<Byte, String> huffmanCodes = generateHuffmanCodes(root);

            // 重置输入流
            bufferedInputStream.reset();

            // 创建一个临时的字节数组输出流来存储压缩数据
            ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream();
            int effectiveBitsLastByte = compressBinaryData(bufferedInputStream, huffmanCodes, tempOutputStream);
            byte[] compressedData = tempOutputStream.toByteArray();

            // 写入压缩数据的长度和最后一个字节中有效位的数量
            dataOutputStream.writeInt(compressedData.length);
            dataOutputStream.writeByte(effectiveBitsLastByte);

            // 将哈夫曼编码表写入压缩文件
            writeHuffmanCodesToStream(huffmanCodes, dataOutputStream);

            // 将压缩后的数据写入输出流
            dataOutputStream.write(compressedData, 0, compressedData.length);

            // 关闭流
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //对原始数据进行哈夫曼编码，并写入输出流
    public static int compressBinaryData(InputStream inputStream, HashMap<Byte, String> huffmanCodes, ByteArrayOutputStream tempOutputStream) throws IOException {
        int bitCount = 0;
        int outputByte = 0;
        int effectiveBitsLastByte = 8;  // 默认情况下，假设最后一个字节完全有效

        byte[] buffer = new byte[1024];
        int bytesRead;

        // 逐块读取数据并应用哈夫曼编码
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                String code = huffmanCodes.get(buffer[i]);
                for (char bit : code.toCharArray()) {
                    outputByte <<= 1;
                    if (bit == '1') {
                        outputByte |= 1;
                    }
                    bitCount++;

                    // 当累积到8位，写入输出流
                    if (bitCount == 8) {
                        tempOutputStream.write(outputByte);
                        bitCount = 0;
                        outputByte = 0;
                    }
                }
            }
        }

        // 处理最后一个字节（如果有）
        if (bitCount > 0) {
            effectiveBitsLastByte = bitCount;
            outputByte <<= (8 - bitCount);  // 左移以填充剩余的位
            tempOutputStream.write(outputByte);
        }

        // 返回最后一个字节中有效位的数量
        return effectiveBitsLastByte;
    }


    //考虑多种异常情况并加入交互
    public static void finalFolderCompression(String inputFilePath, String outputFilePath) {
        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputFilePath);

        if (!inputFile.exists()) {
            System.out.println("文件夹不存在。");
            return;
        }

        if (outputFile.exists()) {
            // 文件存在时的处理逻辑
            System.out.println("输出文件夹已存在。");
            System.out.print("是否要覆盖文件夹？(输入 y 或 n): ");
            // 根据用户选择执行不同的操作
            Scanner scanner = new Scanner(System.in);
            String userChoice = scanner.nextLine();

            if (userChoice.equalsIgnoreCase("y")) {
                // 覆盖文件的逻辑
                if (outputFile.delete()) {
                    System.out.println("旧文件夹已删除，正在进行压缩");
                    compressFolder(inputFilePath, outputFilePath);
                    System.out.println("压缩完毕！");
                } else {
                    System.out.println("旧文件夹删除失败，操作已取消");
                    return;
                }
            } else if (userChoice.equalsIgnoreCase("n")) {
                System.out.println("操作已取消。");
                return;
            } else {
                System.out.println("无效输入。");
                return;
            }
        } else {
            // 文件不存在时的操作
            System.out.println("执行压缩...");
            compressFolder(inputFilePath, outputFilePath);
        }

    }


}
