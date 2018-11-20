package pl.edu.icm.unity.stdext.utils;

import pl.edu.icm.unity.exceptions.InternalException;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Helper class to keep information about image object: the binary value of image and it's format.
 *
 * @author R. Ledzinski
 */
public class BufferedImageWithExt {
    private BufferedImage image;
    private ImageType type;

    public BufferedImageWithExt(BufferedImage image, ImageType type) {
        this.image = image;
        this.type = type;
    }

    public BufferedImageWithExt(BufferedImage image) {
        this(image, ImageType.JPG);
    }

    public enum ImageType {
        JPG("image/jpeg"),
        PNG("image/png"),
        GIF("image/gif"); // Each type has to be 3 letters length - important for serialization

        private String mimeType;

        private ImageType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }

        public static String getSupportedMimeTypes(String delimiter) {
            return Arrays.asList(values()).stream().
                    map(ImageType::getMimeType).
                    collect(Collectors.joining(delimiter));
        }

        public String toExt() {
            return toString().toLowerCase();
        }

        public static ImageType fromMimeType(String mimeType) {
            for(ImageType type : values()) {
                if (type.mimeType.equals(mimeType))
                    return type;
            }
            throw new InternalException("Unsupported mimeType: " + mimeType); // FIXME - need to change, causing web crash
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public ImageType getType() {
        return type;
    }

    public void setType(ImageType type) {
        this.type = type;
    }
}
