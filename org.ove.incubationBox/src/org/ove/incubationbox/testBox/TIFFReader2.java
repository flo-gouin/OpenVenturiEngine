package org.ove.incubationbox.testBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class TIFFReader2 {
   private static final short TIFF_MAGIC_NUMBER = 42;
   private static final short LITTLE_ENDIAN = 0x4949;
   private static final short BIG_ENDIAN = 0x4D4D;
   
   private static final ArrayList<Integer> _dataOffsetList = new ArrayList<>();
   private static final ArrayList<Integer> _dataLengthList = new ArrayList<>();
   
   private static FileChannel _channel;
   private static ByteOrder _byteOrder = ByteOrder.nativeOrder();

   public static void read(File file_p) {
      FileInputStream fis;
      try {
         fis = new FileInputStream(file_p);
         _channel = fis.getChannel();
         
         ByteBuffer fileheader = ByteBuffer.allocateDirect(4);

         _channel.read(fileheader);
         fileheader.flip();

         // Get File ByteOrder
         switch (fileheader.getShort()) {
            case LITTLE_ENDIAN:
               _byteOrder = ByteOrder.LITTLE_ENDIAN;
               break;
            case BIG_ENDIAN:
               _byteOrder = ByteOrder.BIG_ENDIAN;
               break;
            default:
               System.err.println("Unknown endianess");
               return;
         }
         fileheader.order(_byteOrder);

         // Check if the file is a valid TIFF File
         if (fileheader.getShort() != TIFF_MAGIC_NUMBER) {
            System.err.println(file_p + " isn't a valid TIFF file");
         }

         // Get IFD offset
         ByteBuffer ifdBuffer = ByteBuffer.allocateDirect(4);
         ifdBuffer.order(_byteOrder);
         _channel.read(ifdBuffer);
         ifdBuffer.flip();
         int position = ifdBuffer.getInt();

         System.out.println("IFD start position:" + position);

         _channel.position(position);

         // Get quantity of IFD
         ByteBuffer ifdQtyBuffer = ByteBuffer.allocateDirect(2);
         ifdQtyBuffer.order(_byteOrder);
         _channel.read(ifdQtyBuffer);
         ifdQtyBuffer.flip();
         int qty = ifdQtyBuffer.getShort();

         System.out.println("IFD qty:" + qty);

         for (int ifdNum = 1; ifdNum <= qty; ifdNum++) {
            ByteBuffer ifdEntryBuffer = ByteBuffer.allocateDirect(12);
            ifdEntryBuffer.order(_byteOrder);
            _channel.read(ifdEntryBuffer);
            ifdEntryBuffer.flip();

            short tagId = ifdEntryBuffer.getShort();
            short fieldType = ifdEntryBuffer.getShort();
            int nbValues = ifdEntryBuffer.getInt();
            int value = ifdEntryBuffer.getInt();

            System.out.println("IFD #"
                       + ifdNum
                       + " ["
                       + tagId
                       + "] -> "
                       + nbValues
                       + " "
                       + decodeType(fieldType)
                       + " value(s):");
            decode(tagId, fieldType, nbValues, value);
         }

         ByteBuffer nextIfdOffsetBuffer = ByteBuffer.allocateDirect(4);
         nextIfdOffsetBuffer.order(_byteOrder);
         _channel.read(nextIfdOffsetBuffer);
         nextIfdOffsetBuffer.flip();

         System.out.println("Next offset:" + nextIfdOffsetBuffer.getInt());
            
         System.out.println(">>>>Get datas");
         if(_dataLengthList.size()==_dataOffsetList.size()){
            for(int numDataBlock = 0; numDataBlock<_dataOffsetList.size(); numDataBlock++){
               System.out.println("Reading data block #"+numDataBlock);
               int offset = _dataOffsetList.get(numDataBlock);
               int size = _dataLengthList.get(numDataBlock);
               System.out.println("Reading to the offset #"+offset+" for "+size+"bytes");
               _channel.position(offset);
               ByteBuffer buffer = ByteBuffer.allocateDirect(size);
               buffer.order(_byteOrder);
               _channel.read(buffer);
            }
         }else{
            System.err.println("Incoherent quantity of offset and length datas");
         }
         
         System.out.println("End of TIFF file reader");
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private static void decode(short tagId_p, short fieldType_p, int nbValues_p, int value_p)
                                                                                            throws IOException {
      long backUpPosition = 0;
      ByteBuffer buffer;
      switch (tagId_p) {
         //ImageWidth
         case 256:
            System.out.println("Width: " + value_p + "px");
            break;
         //ImageHeight
         case 257:
            System.out.println("Height: " + value_p + "px");
            break;
         //BitsPerSample [Grayscale Images]
         case 258:
            System.out.println("BitsPerSample: " + value_p + " bits"); // Not used with Bilevel images
            break;
         //Compression
         case 259:
            decodeCompression(fieldType_p, nbValues_p, value_p);
            break;
         //PhotometricInterpretation
         case 262:
            decodeColor(fieldType_p, nbValues_p, value_p);
            break;
         //StripOffsets
         case 273:
            System.out.println("#0 offset: " + value_p);
            _dataOffsetList.add(value_p);
            
//            backUpPosition = _channel.position();
//
//            _channel.position(value_p);
//            buffer = ByteBuffer.allocateDirect(nbValues_p * 4);
//            buffer.order(_byteOrder);
//            _channel.read(buffer);
//            buffer.flip();
//
//            for (int cpt = 0; cpt < nbValues_p; cpt++) {
//               int offset = buffer.getInt();
            // System.out.println("#" + cpt + " offset: " + offset);
            // _dataOffsetList.add(offset);
//            }

//            _channel.position(backUpPosition);
            break;
         case 274:
            decodeOrientation(fieldType_p, nbValues_p, value_p);
            break;
         //SampesPerPixel [RGB FullColor Images]
         case 277:
            System.out.println("SamplesPerPixel: " + value_p + " component(s)");
            break;
         //RowsPerStrip
         case 278:
            System.out.println("Nb row per strips: " + value_p);
            break;
         //StripByteCounts
         case 279:
            System.out.println("#0 size: " + value_p);
            _dataLengthList.add(value_p);
            
//            backUpPosition = _channel.position();
//
//            _channel.position(value_p);
//            buffer = ByteBuffer.allocateDirect(nbValues_p * 4);
//            buffer.order(_byteOrder);
//            _channel.read(buffer);
//            buffer.flip();
//
//            for (int cpt = 0; cpt < nbValues_p; cpt++) {
//               int length = buffer.getInt();
//               System.out.println("#" + cpt + " size: " + length);
//               _dataLengthList.add(length);
//            }
//
//            _channel.position(backUpPosition);
            break;
         //XResolution
         case 282:
            backUpPosition = _channel.position();

            _channel.position(value_p);
            buffer = ByteBuffer.allocateDirect(2 * 4);
            buffer.order(_byteOrder);
            _channel.read(buffer);
            buffer.flip();

            _channel.position(backUpPosition);

            System.out.println("XResolution: " + (buffer.getInt() / (float) (buffer.getInt())));
            break;
         //YResolution
         case 283:
            backUpPosition = _channel.position();

            _channel.position(value_p);
            buffer = ByteBuffer.allocateDirect(2 * 4);
            buffer.order(_byteOrder);
            _channel.read(buffer);
            buffer.flip();

            _channel.position(backUpPosition);

            System.out.println("YResolution: " + (buffer.getInt() / (float) (buffer.getInt())));
            break;
         case 284:
            decodePlanarConfiguration(fieldType_p, nbValues_p, value_p);
            break;
         //ResolutionUnit
         case 296:
            decodeResolutionUnit(fieldType_p, nbValues_p, value_p);
            break;
         case 315:
            //TODO complete this part
            break;
         //ColorMap [Palette-color Images]
         case 320:
            System.out.println("Color map: " + value_p); // Specific palette-color Image
         default:
            System.out.println("<<Unknown Tag [" + tagId_p + "]>>");
            break;
      }
   }

   private static void decodePlanarConfiguration(short fieldType_p, int nbValues_p, int value_p) {
      switch (value_p) {
         case 1: // Default value
            System.out.println("Chunky format storage");
            break;
         case 2:
            System.out.println("Planar format storage");
            break;
         default:
            System.out.println("Unknown Value: " + value_p);
            break;
      }
   }

   private static void decodeOrientation(short fieldType_p, int nbValues_p, int value_p) {
      switch (value_p) {
         case 1: // Default value
            System.out.println("Top left origin");
            break;
         case 2:
            System.out.println("Top right origin");
            break;
         case 3:
            System.out.println("Bottom right origin");
            break;
         case 4:
            System.out.println("Bottom left origin");
            break;
         case 5:
            System.out.println("x<->y inverted top left origin");
            break;
         case 6:
            System.out.println("x<->y inverted top right origin");
            break;
         case 7:
            System.out.println("x<->y inverted bottom right origin");
            break;
         case 8:
            System.out.println("x<->y inverted bottom left origin");
            break;
         default:
            System.out.println("Unknown Value: " + value_p);
            break;
      }
   }

   private static String decodeType(short fieldType_p) {
      switch (fieldType_p) {
         case 1:
            return "byte";
         case 2:
            return "ASCII";
         case 3:
            return "short";
         case 4:
            return "long";
         case 5:
            return "rational";
         case 6:
            return "sbyte";
         case 7:
            return "undefined";
         case 8:
            return "sshort";
         case 9:
            return "slong";
         case 10:
            return "srational";
         case 11:
            return "float";
         case 12:
            return "double";
         default:
            return "unknown(" + fieldType_p + ")";
      }
   }

   private static void decodeResolutionUnit(short fieldType_p, int nbValues_p, int value_p) {
      switch (value_p) {
         case 1:
            System.out.println("No absolute unit");
            break;
         case 2:
            System.out.println("Inch unit"); // Default value
            break;
         case 3:
            System.out.println("Centimeter unit");
            break;
         default:
            System.out.println("Unknown Value: " + value_p);
            break;
      }
   }

   private static void decodeColor(short fieldType_p, int nbValues_p, int value_p) {
      switch (value_p) {
         case 0:
            System.out.println("White is zero");
            break;
         case 1:
            System.out.println("Black is zero");
            break;
         case 2:
            System.out.println("RGB");
            break;
         case 3:
            System.out.println("Palette color");
            break;
         case 4:
            System.out.println("Transparency mask");
            break;
         default:
            System.out.println("Unknown Value: " + value_p);
            break;
      }
   }

   private static void unpackData(FileChannel channel_p,
                                  long offset_p,
                                  long length_p,
                                  ByteBuffer destinationBuffer_p) throws IOException {
      long backUpPosition = _channel.position();

      channel_p.position(offset_p);

      ByteBuffer headBuffer = ByteBuffer.allocateDirect(1);
      headBuffer.order(_byteOrder);

      long nbReadBytes = 0;

      while (nbReadBytes < length_p) {
         headBuffer.clear();
         nbReadBytes += channel_p.read(headBuffer);
         headBuffer.flip();

         byte head = headBuffer.get();

         if (head <= Byte.MIN_VALUE) {
            continue;
         } else if (head > 0) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1 + head);
            buffer.order(_byteOrder);
            nbReadBytes += channel_p.read(buffer);
            buffer.flip();

            destinationBuffer_p.put(buffer);
         } else {
            ByteBuffer buffer = ByteBuffer.allocateDirect(1);
            buffer.order(_byteOrder);
            nbReadBytes += channel_p.read(buffer);
            buffer.flip();

            byte data = buffer.get();

            for (int cpt = 0; cpt < (1 - head); cpt++) {
               destinationBuffer_p.put(data);
            }
         }
      }

      _channel.position(backUpPosition);
   }

   private static void decodeCompression(short fieldType_p, int nbValues_p, int value_p) {
      switch (value_p) {
         case 1: // Default value
            System.out.println("No Compression");
            break;
         case 2:
            System.out.println("Modified Huffman Compression");
            break;
         case 32773:
            System.out.println("PackBits Compression");
            break;
         default:
            System.out.println("Unknown Value: " + value_p);
            break;
      }
   }
}
