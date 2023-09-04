import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BencodeUtils {

    public static byte[] encode(Object data) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        encode(data, outputStream);
        return outputStream.toByteArray();
    }

    private static void encode(Object data, ByteArrayOutputStream outputStream) throws IOException {
        if (data instanceof Integer) {
            outputStream.write("i".getBytes(StandardCharsets.UTF_8));
            outputStream.write(data.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.write("e".getBytes(StandardCharsets.UTF_8));
        } else if (data instanceof String) {
            byte[] bytes = ((String) data).getBytes(StandardCharsets.UTF_8);
            outputStream.write(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
            outputStream.write(":".getBytes(StandardCharsets.UTF_8));
            outputStream.write(bytes);
        } else if (data instanceof List) {
            outputStream.write("l".getBytes(StandardCharsets.UTF_8));
            for (Object item : (List<?>) data) {
                encode(item, outputStream);
            }
            outputStream.write("e".getBytes(StandardCharsets.UTF_8));
        } else if (data instanceof Map) {
            outputStream.write("d".getBytes(StandardCharsets.UTF_8));
            Map<String, Object> map = new TreeMap<>((Map<String, Object>) data);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                encode(entry.getKey(), outputStream);
                encode(entry.getValue(), outputStream);
            }
            outputStream.write("e".getBytes(StandardCharsets.UTF_8));
        }
    }

    public static Object decode(byte[] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return decode(inputStream);
    }

    private static Object decode(ByteArrayInputStream inputStream) throws IOException {
        int nextByte = inputStream.read();
        char marker = (char) nextByte;

        if (Character.isDigit(marker)) {
            // 字节串
            int colonIndex = inputStream.available() - 1;
            while (colonIndex > 0 && Character.isDigit(inputStream.available())) {
                colonIndex--;
            }
            byte[] lengthBytes = new byte[colonIndex];
            inputStream.read(lengthBytes);

            inputStream.read(); // 读取冒号
            int length = Integer.parseInt(new String(lengthBytes, StandardCharsets.UTF_8));
            byte[] bytes = new byte[length];
            inputStream.read(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } else if (marker == 'i') {
            // 整数
            StringBuilder integerBuilder = new StringBuilder();
            while (true) {
                nextByte = inputStream.read();
                if (nextByte == 'e') {
                    break;
                }
                integerBuilder.append((char) nextByte);
            }
            return Integer.parseInt(integerBuilder.toString());
        } else if (marker == 'l') {
            // 列表
            List<Object> list = new ArrayList<>();
            while (true) {
                Object item = decode(inputStream);
                if (item == null) {
                    break;
                }
                list.add(item);
            }
            return list;
        } else if (marker == 'd') {
            // 字典
            Map<String, Object> map = new HashMap<>();
            while (true) {
                Object key = decode(inputStream);
                if (key == null) {
                    break;
                }
                Object value = decode(inputStream);
                map.put((String) key, value);
            }
            return map;
        }

        return null;
    }

    // Add decoding logic if needed

    public static void main(String[] args) throws IOException {
//        List<Object> dataList = new ArrayList<>();
//        dataList.add(123);
//        dataList.add("hello");
//        dataList.add(List.of("item1", "item2"));
//
//        Map<String, Object> dataMap = new TreeMap<>();
//        dataMap.put("key1", "value1");
//        dataMap.put("key2", 456);
//
//        dataList.add(dataMap);
//
//        byte[] encodedData = encode(dataList);
        String s = "li123e5:hellol5:item15:item2ed4:key16:value14:key2i456eee";
        System.out.println(decode(s.getBytes()));
    }
}
