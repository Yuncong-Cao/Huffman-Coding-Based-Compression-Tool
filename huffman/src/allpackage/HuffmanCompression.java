package allpackage;

import java.io.*;
import java.util.*;

public class HuffmanCompression {
    // 魔术数字用于单个文件
    public static final byte[] FILE_MAGIC_NUMBER = {0x48, 0x46, 0x49, 0x4C, 0x45}; // "HFILE"
    // 魔术数字用于文件夹
    public static final byte[] FOLDER_MAGIC_NUMBER = {0x48, 0x46, 0x4F, 0x4C, 0x44}; // "HFOLD"
    //定义哈夫曼树的Node结点
    static class Node implements Comparable<Node> {
        Byte data;
        int frequency;
        Node left, right;

        public Node(Byte data, int frequency) {
            this.data = data;
            this.frequency = frequency;
            left = null;
            right = null;
        }

        @Override
        public int compareTo(Node o) {
            return this.frequency - o.frequency;
        }
    }

        //根据每个文件构造相应的哈夫曼树
        public static Node buildHuffmanTree(byte[] fileData) {
            HashMap<Byte, Integer> frequencyMap = new HashMap<>();

            // 统计每个字节的频率
            for (byte b : fileData) {
                frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
            }

            PriorityQueue<Node> priorityQueue = new PriorityQueue<>(Comparator
                    .<Node, Integer>comparing(node -> node.frequency)
                    .thenComparing(node -> (int) node.data)
            );

            // 创建节点并添加到优先队列中
            for (byte key : frequencyMap.keySet()) {
                priorityQueue.add(new Node(key, frequencyMap.get(key)));
            }

            // 构建哈夫曼树
            while (priorityQueue.size() > 1) {
                Node left = priorityQueue.poll();
                Node right = priorityQueue.poll();

                Node parent = new Node((byte) '\0', left.frequency + right.frequency);
                parent.left = left;
                parent.right = right;

                priorityQueue.add(parent);
            }

            // 返回根节点
            return priorityQueue.poll();
        }



    //根据哈夫曼树生成哈夫曼编码表
    public static HashMap<Byte, String> generateHuffmanCodes(Node root) {
        HashMap<Byte, String> huffmanCodes = new HashMap<>();
        generateCodes(root, "", huffmanCodes);
        return huffmanCodes;
    }
    //用递归的方法
    private static void generateCodes(Node root, String code, HashMap<Byte, String> huffmanCodes) {
        if (root == null) {
            return;
        }

        if (isLeaf(root)) {
            huffmanCodes.put((byte)root.data, code);
        }

        generateCodes(root.left, code + "0", huffmanCodes);
        generateCodes(root.right, code + "1", huffmanCodes);
    }
    public static boolean isLeaf(Node root) {
        return root.left == null && root.right == null;
    }


    //将哈夫曼编码表写入压缩文件，以便于解压缩
    public static void writeHuffmanCodesToStream(Map<Byte, String> huffmanCodes, DataOutputStream dataOutputStream) {
        try {
            dataOutputStream.writeInt(huffmanCodes.size()); // 写入哈夫曼编码表的大小
            for (Map.Entry<Byte, String> entry : huffmanCodes.entrySet()) {
                dataOutputStream.writeByte(entry.getKey()); // 写入字节
                String code = entry.getValue();
                dataOutputStream.writeInt(code.length()); // 先写入编码长度
                for (char c : code.toCharArray()) { // 写入编码的二进制形式
                    dataOutputStream.writeChar(c);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 对原始数据进行哈夫曼编码，并写入输出流
    public static void compressBinaryData(InputStream inputStream, HashMap<Byte, String> huffmanCodes, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024]; // 缓冲区大小可以调整
        int bytesRead;

        int bitCount = 0;
        int outputByte = 0;

        // 逐块读取数据并应用哈夫曼编码
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                String code = huffmanCodes.get(buffer[i]);
                for (char bit : code.toCharArray()) {
                    outputByte = (outputByte << 1) | (bit == '1' ? 1 : 0);
                    bitCount++;

                    // 当累积到8位，写入输出流
                    if (bitCount == 8) {
                        outputStream.write(outputByte);
                        bitCount = 0;
                        outputByte = 0;
                    }
                }
            }
        }
        // 如果最后不足8位，需要填充并写入剩余的位
        if (bitCount > 0) {
            outputByte <<= (8 - bitCount); // 左移以填充剩余的位
            outputStream.write(outputByte);
        }
    }


    public static byte[] convertInputStreamToByteArray(BufferedInputStream bufferedInputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        return byteArrayOutputStream.toByteArray();
    }


    //压缩文件
    public static void compressFile(String inputFilePath, String outputFilePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(inputFilePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);


            // 写入 magic number
            dataOutputStream.write(FILE_MAGIC_NUMBER);

            // 写入文件名
            dataOutputStream.writeUTF(new File(inputFilePath).getName());

            // 构建哈夫曼树
            bufferedInputStream.mark(Integer.MAX_VALUE);
            Node root = buildHuffmanTree(convertInputStreamToByteArray(bufferedInputStream));

            // 生成哈夫曼编码表
            HashMap<Byte, String> huffmanCodes = generateHuffmanCodes(root);

            // 将哈夫曼编码表写入压缩文件
            writeHuffmanCodesToStream(huffmanCodes, dataOutputStream);

            // 重置输入流
            bufferedInputStream.reset();

            // 将哈夫曼编码后的数据写入压缩文件
            compressBinaryData(bufferedInputStream, huffmanCodes, bufferedOutputStream);

            // 关闭流
            bufferedInputStream.close();
            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //考虑多种异常情况并加入交互
    public static void finalHuffmanCompression(String inputFilePath, String outputFilePath) {
        File inputFile = new File(inputFilePath);
        File outputFile = new File(outputFilePath);

        if (!inputFile.exists()) {
            System.out.println("文件不存在。");
            return;
        }

        if (outputFile.exists()) {
            // 文件存在时的处理逻辑
            System.out.println("输出文件已存在。");
            System.out.print("是否要覆盖文件？(输入 y 或 n): ");
            // 根据用户选择执行不同的操作
            Scanner scanner = new Scanner(System.in);
            String userChoice = scanner.nextLine();

            if (userChoice.equalsIgnoreCase("y")) {
                // 覆盖文件的逻辑
                if (outputFile.delete()) {
                    System.out.println("旧文件已删除，正在进行压缩");
                    compressFile(inputFilePath, outputFilePath);
                    System.out.println("压缩完毕！");
                } else {
                    System.out.println("旧文件删除失败，操作已取消");
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
            compressFile(inputFilePath, outputFilePath);
        }
    }

}