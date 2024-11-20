package org.example.utilities;

import java.io.*;

public class Serialization {
    public static byte[] serializeObjectToByteArray(Serializable obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);  // byteArrayOutputStream에 데이터를 씀
        objectOutputStream.close();
        return byteArrayOutputStream.toByteArray();
    }

    public static <T extends Serializable> T deserializeObjectFromByteArray(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        @SuppressWarnings("unchecked")
        T obj = (T) objectInputStream.readObject();
        objectInputStream.close();
        return obj;
    }
}
