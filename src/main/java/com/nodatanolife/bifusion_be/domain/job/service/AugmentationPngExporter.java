package com.nodatanolife.bifusion_be.domain.job.service;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 증강 결과 데이터(npz)의 이미지를 PNG로 바꿔 원본 npz와 함께 zip 하나로 묶는다.
 * npz는 ML이 np.savez_compressed(x=, y=, ids=)로 만든 zip이다.
 * - x: float32 (N, C, H, W), 값 0~1. C=1 흑백, C=3 RGB
 * - y: int64 (N), 0 정상 / 1 비정상
 * - ids는 pickle로 저장된 object 배열이라 읽지 않는다. ML과 같은 규칙(normal_0000 …)으로 y 순서에서 이름을 만든다.
 * 형식이 다르면 잘못된 이미지를 만들지 않도록 IOException을 던진다.
 */
@Component
public class AugmentationPngExporter {

    static final String NPZ_ENTRY_NAME = "support_aug.npz";

    private static final long NORMAL = 0;
    private static final long ANOMALY = 1;

    /**
     * @param npz 증강 결과 npz 파일
     * @param zip 만들 zip 파일 (있으면 덮어쓴다)
     */
    public void export(Path npz, Path zip) throws IOException {
        try (ZipFile npzFile = new ZipFile(npz.toFile());
             ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zip)))) {
            // npz와 PNG는 이미 압축돼 있어 다시 압축해도 거의 줄지 않는다
            out.setLevel(Deflater.BEST_SPEED);
            long[] labels = readLabels(npzFile);
            out.putNextEntry(new ZipEntry(NPZ_ENTRY_NAME));
            Files.copy(npz, out);
            out.closeEntry();
            writeImages(npzFile, labels, out);
        }
    }

    private static long[] readLabels(ZipFile npz) throws IOException {
        try (DataInputStream in = open(npz, "y.npy")) {
            NpyHeader header = NpyHeader.read(in);
            if (!header.descr().equals("<i8") || header.shape().length != 1) {
                throw new IOException("y 배열 형식이 다름: " + header);
            }
            long[] labels = new long[header.shape()[0]];
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < labels.length; i++) {
                in.readFully(buffer.array());
                labels[i] = buffer.getLong(0);
                if (labels[i] != NORMAL && labels[i] != ANOMALY) {
                    throw new IOException("알 수 없는 라벨: " + labels[i]);
                }
            }
            return labels;
        }
    }

    private static void writeImages(ZipFile npz, long[] labels, ZipOutputStream out) throws IOException {
        try (DataInputStream in = open(npz, "x.npy")) {
            NpyHeader header = NpyHeader.read(in);
            int[] shape = header.shape();
            if (!header.descr().equals("<f4") || shape.length != 4 || shape[0] != labels.length
                    || (shape[1] != 1 && shape[1] != 3)) {
                throw new IOException("x 배열 형식이 다름: " + header);
            }
            int channels = shape[1];
            int height = shape[2];
            int width = shape[3];
            ByteBuffer pixels = ByteBuffer.allocate(channels * height * width * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
            int normalIndex = 0;
            int anomalyIndex = 0;
            for (long label : labels) {
                in.readFully(pixels.array());
                BufferedImage image = toImage(pixels.asFloatBuffer(), channels, height, width);
                String name = label == NORMAL
                        ? String.format("png/normal/normal_%04d.png", normalIndex++)
                        : String.format("png/anomaly/anomaly_%04d.png", anomalyIndex++);
                out.putNextEntry(new ZipEntry(name));
                ImageIO.write(image, "png", out);
                out.closeEntry();
            }
        }
    }

    /**
     * 채널 우선(C, H, W) 배열 한 장을 이미지로 바꾼다.
     */
    private static BufferedImage toImage(FloatBuffer values, int channels, int height, int width) {
        int plane = height * width;
        if (channels == 1) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = image.getRaster();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    raster.setSample(x, y, 0, toByte(values.get(y * width + x)));
                }
            }
            return image;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = y * width + x;
                int r = toByte(values.get(offset));
                int g = toByte(values.get(plane + offset));
                int b = toByte(values.get(2 * plane + offset));
                image.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    // ML이 0~255를 255로 나눠 저장했으므로 되돌린다. 범위 밖 값은 잘라낸다.
    private static int toByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255f)));
    }

    private static DataInputStream open(ZipFile npz, String entryName) throws IOException {
        ZipEntry entry = npz.getEntry(entryName);
        if (entry == null) {
            throw new IOException("npz에 " + entryName + " 없음");
        }
        return new DataInputStream(new BufferedInputStream(npz.getInputStream(entry)));
    }

    /**
     * .npy 헤더. 형식: 매직(\x93NUMPY) + 버전 2바이트 + 헤더 길이(v1: 2바이트, v2 이상: 4바이트, little endian) + Python dict 문자열.
     */
    private record NpyHeader(String descr, int[] shape) {

        private static final byte[] MAGIC = {(byte) 0x93, 'N', 'U', 'M', 'P', 'Y'};
        private static final Pattern DESCR = Pattern.compile("'descr':\\s*'([^']+)'");
        private static final Pattern FORTRAN_ORDER = Pattern.compile("'fortran_order':\\s*(True|False)");
        private static final Pattern SHAPE = Pattern.compile("'shape':\\s*\\(([^)]*)\\)");

        static NpyHeader read(DataInputStream in) throws IOException {
            byte[] magic = new byte[MAGIC.length];
            in.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new IOException("npy 파일이 아님");
            }
            int major = in.readUnsignedByte();
            in.readUnsignedByte();
            byte[] lengthBytes = new byte[major == 1 ? 2 : 4];
            in.readFully(lengthBytes);
            ByteBuffer length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN);
            int headerLength = major == 1 ? Short.toUnsignedInt(length.getShort()) : length.getInt();
            byte[] headerBytes = new byte[headerLength];
            in.readFully(headerBytes);
            String header = new String(headerBytes, major >= 3 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1);

            Matcher descr = DESCR.matcher(header);
            Matcher fortranOrder = FORTRAN_ORDER.matcher(header);
            Matcher shape = SHAPE.matcher(header);
            if (!descr.find() || !fortranOrder.find() || !shape.find()) {
                throw new IOException("npy 헤더를 읽을 수 없음: " + header);
            }
            if (fortranOrder.group(1).equals("True")) {
                throw new IOException("fortran_order 배열은 지원하지 않음");
            }
            int[] dims = Arrays.stream(shape.group(1).split(","))
                    .map(String::trim)
                    .filter(dim -> !dim.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .toArray();
            return new NpyHeader(descr.group(1), dims);
        }

        @Override
        public String toString() {
            return "descr=" + descr + ", shape=" + Arrays.toString(shape);
        }
    }
}
