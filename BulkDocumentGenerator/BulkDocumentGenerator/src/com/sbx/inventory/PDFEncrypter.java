package com.sbx.inventory;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.PNGEncodeParam;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;

public class PDFEncrypter {
	
	public static final void encryptPDF(String inFileName, String outFileName, String userPassword, String ownerPassword) throws Exception {
		PDDocument doc = PDDocument.load(new File(inFileName));
		addProcetion(doc, userPassword, ownerPassword);
		doc.save(outFileName);
		doc.close();		
	}
	

	
	public final static void createEncrpytedPDFFromTIFF(String pdfFileName, String[] tiffFiles, String userPassword, String ownerPassword) throws Exception{
		createEncrpytedPDFFromTIFF(pdfFileName, tiffFiles,userPassword,ownerPassword,null);
	}
	
	public final static void createEncrpytedPDFFromTIFF(String pdfFileName, String[] tiffFiles, String userPassword, String ownerPassword, PDDocumentInformation info) throws Exception{
		PDDocument pdf = new PDDocument();
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmss"); 
		int imgCnt = 0;
		for (int i = 0; i<tiffFiles.length; i++) {
			//Go through all the images
			File tiffFile = new File(tiffFiles[i]);
			if (! tiffFile.exists()) continue;
			SeekableStream s = new FileSeekableStream(tiffFiles[i]);
	        TIFFDecodeParam param = null;
	        ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
	        for (int p = 0; p<dec.getNumPages(); p++) {
	        	try {
	        		String jpegFileName = tiffFiles[i] + sdf.format(new Date()) + "_" + p + ".jpg"; 
		            RenderedImage op = dec.decodeAsRenderedImage(p);
		            FileOutputStream fos = new FileOutputStream(jpegFileName);
		            JPEGEncodeParam jpgparam = new JPEGEncodeParam();
		            jpgparam.setQuality(67);
		            //pngParam.setCompressedText(PNGEncodeParam.);
		            ImageEncoder en = ImageCodec.createImageEncoder("jpeg", fos, null);
		            en.encode(op);
		            fos.flush();
		            fos.close();		
		            PDPage page = new PDPage();
		            PDImageXObject img = PDImageXObject.createFromFileByContent(new File(jpegFileName), pdf);
					PDPageContentStream contentStream = new PDPageContentStream(pdf, page);
					contentStream.drawImage(img, 1, 1,page.getMediaBox().getWidth()-10,page.getMediaBox().getHeight()-10);
					contentStream.close();
					pdf.addPage(page);
					imgCnt++;
					(new File(jpegFileName)).delete();
	        	} catch (Exception ex) {
	        		ex.printStackTrace();
	        	}

	        }
		}
		if (imgCnt == 0) {
			pdf.close();
			throw new Exception("No image was imported to pdf.");
		}
		addProcetion(pdf, userPassword, ownerPassword);
		if (info != null) pdf.setDocumentInformation(info);
		pdf.save(new File(pdfFileName));
		pdf.close();
	}
	
	private final static void convertTiff2JPG(String tiffFileName, String jpegFileName) throws IOException{
		SeekableStream s = new FileSeekableStream(tiffFileName);
        TIFFDecodeParam param = null;
        ImageDecoder dec = ImageCodec.createImageDecoder("tiff", s, param);
        RenderedImage op = dec.decodeAsRenderedImage(0);
        FileOutputStream fos = new FileOutputStream(jpegFileName);
        JPEGEncodeParam jpgparam = new JPEGEncodeParam();
        jpgparam.setQuality(67);
        ImageEncoder en = ImageCodec.createImageEncoder("jpeg", fos, jpgparam);
        en.encode(op);
        fos.flush();
        fos.close();		
	}

	private static final void addProcetion(PDDocument doc, String userPassword, String ownerPassword) throws Exception {
		int keyLength = 128;
		AccessPermission ap = new AccessPermission();

		ap.setCanPrint(true);
		ap.setCanModify(false);
		ap.setCanAssembleDocument(false);

		if (ownerPassword.length() == 0 && userPassword.length()>0) ownerPassword = "9a9b8c8d7e7f";
		
		StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
		spp.setEncryptionKeyLength(keyLength);
		spp.setPermissions(ap);
		doc.protect(spp);
		
	}
	
	public static void main(String[] args) {
		try {
			//PDFEncrypter.encryptPDF("tmp/2.pdf", "pdfs/2.pdf", "sbx7575", "owner");
			PDFEncrypter.createEncrpytedPDFFromTIFF("pdfs/test.pdf", new String[] {"tmp/TEST.TIFF"}, "sbx7575", "asdf");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}


