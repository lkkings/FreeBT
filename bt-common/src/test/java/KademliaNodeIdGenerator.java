import java.security.SecureRandom;
import java.util.Arrays;

public class KademliaNodeIdGenerator {
    private static final int NODE_ID_LENGTH = 160; // 节点 ID 的长度（位数）
    private static final int NEIGHBOR_END = 32; // 截取目标节点 ID 的前部分长度

    public static void main(String[] args) {
        String targetNodeId = "00112233445566778899AABBCCDDEEFF"; // 目标节点 ID
        String neighborNodeId = generateNeighborNodeId(targetNodeId);
        System.out.println("目标节点 ID: " + targetNodeId);
        System.out.println("邻居节点 ID: " + neighborNodeId);
    }

    public static String generateNeighborNodeId(String targetNodeId) {
        SecureRandom random = new SecureRandom();
        byte[] targetBytes = hexStringToByteArray(targetNodeId);
        byte[] neighborBytes = Arrays.copyOf(targetBytes, NODE_ID_LENGTH / 8);

        for (int i = NEIGHBOR_END / 8; i < NODE_ID_LENGTH / 8; i++) {
            neighborBytes[i] = (byte) (neighborBytes[i] ^ random.nextInt(256));
        }

        return byteArrayToHexString(neighborBytes);
    }

    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }
}
