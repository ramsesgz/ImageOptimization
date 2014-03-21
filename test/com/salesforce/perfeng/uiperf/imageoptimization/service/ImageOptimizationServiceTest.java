package com.salesforce.perfeng.uiperf.imageoptimization.service;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import com.salesforce.perfeng.uiperf.imageoptimization.dto.OptimizationResult;
import com.salesforce.perfeng.uiperf.imageoptimization.service.IImageOptimizationService.FileTypeConversion;
import com.salesforce.perfeng.uiperf.imageoptimization.utils.FixedFileUtils;
import com.salesforce.perfeng.uiperf.imageoptimization.utils.ImageFileOptimizationException;
import com.salesforce.perfeng.uiperf.imageoptimization.utils.ImageUtils;

/**
 * Test for {@link ImageOptimizationService}.
 * 
 * @author eperret (Eric Perret)
 * @since 186.internal
 */
public class ImageOptimizationServiceTest {
	
	private static final String WEBP_ID = "|webp";
	private static final String DEFAULT_BINARY_APP_LOCATION;
	static {
		if ("linux".equals(System.getProperty("os.name").toLowerCase())) {
			DEFAULT_BINARY_APP_LOCATION = "./lib/binary/linux/";
		} else {
			throw new UnsupportedOperationException("Your OS is not supported by this application. Currently only linux is supported");
		}
	}
	
	private ImageOptimizationService<Object> imageOptimizationService;
	
	/**
	 * Used to initialize the {@link ImageOptimizationService} used by all of 
	 * the tests.
	 * 
	 * @throws IOException Thrown if there is a problem trying to initialize the
	 *                     directories used by 
	 *                     {@link ImageOptimizationService#ImageOptimizationService(File, File)}.
	 */
	@Before
    public void setUp() throws IOException {
		final File tmpDir = File.createTempFile(ImageOptimizationServiceTest.class.getName(), "");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpDir.deleteOnExit();
		
		imageOptimizationService = new ImageOptimizationService<>(tmpDir, new File(DEFAULT_BINARY_APP_LOCATION));
    }
	
	/**
	 * Test method for
	 * {@link ImageOptimizationService#ImageOptimizationService(File, File)}.
	 * 
	 * @throws IOException Can be thrown by the
	 *                     <code>ImageOptimizationService</code> constructor if 
	 *                     its passed in file has an issue.
	 */
	@SuppressWarnings("unused")
	@Test
	public void testImageOptimizationService() throws IOException {
		try {
			new ImageOptimizationService<>(File.createTempFile("qqq", "qqq"), new File(DEFAULT_BINARY_APP_LOCATION));
			fail("It is expected that the file does not exist.");
		} catch(final IllegalArgumentException ignore) {
			// if this catch block is executed then the test passed and the File
			// does not exist.
		}
		try {
			new ImageOptimizationService<>(null, new File(DEFAULT_BINARY_APP_LOCATION));
			fail("It is expected that the file does not exist.");
		} catch(final IllegalArgumentException ignore) {
			// if this catch block is executed then the test passed and the File
			// does not exist.
		}
		try {
			final File file = File.createTempFile("qqq", "qqq");
			file.createNewFile();
			file.deleteOnExit();
			new ImageOptimizationService<>(file, new File(DEFAULT_BINARY_APP_LOCATION));
			fail("failed because we expected tthe temp directory to be a file instead of a directory.");
		} catch(final IllegalArgumentException ignore) {
			// if this catch block is executed then the test passed and the File
			// is a file instead of a directory.
		}
		
		final File tmpDir = File.createTempFile(ImageOptimizationServiceTest.class.getName(), "");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpDir.deleteOnExit();
		assertNotNull(new ImageOptimizationService<>(tmpDir, new File(DEFAULT_BINARY_APP_LOCATION)));
	}

	private static final void validateFileOptimization(final OptimizationResult<Object> result, final ImageOptimizationTestDTO imageOptimizationTestDTO, final boolean isWebP) throws IOException {
		final String errorMsg = String.format("failed for image \"%s\"", imageOptimizationTestDTO.getMasterFile().getName());
		
		// Checking that the master image was not updated as part of this 
		// process.
		assertEquals(errorMsg, imageOptimizationTestDTO.getMasterFileChecksum(), FileUtils.checksumCRC32(imageOptimizationTestDTO.getMasterFile()));
		
		if(imageOptimizationTestDTO.isOptimized() || isWebP) {
		
			assertNotNull(errorMsg, result);
			
			assertNull(errorMsg, result.getGusBugId());
			assertNull(errorMsg, result.getNewChangeList());
			assertNotNull(errorMsg, result.getOptimizedFile());
			assertTrue(errorMsg, result.getOptimizedFile().exists());
			
			if(isWebP) {
				assertEquals(errorMsg, FilenameUtils.removeExtension(imageOptimizationTestDTO.getMasterFile().getName()), FilenameUtils.removeExtension(result.getOptimizedFile().getName()));
				assertEquals(errorMsg, IImageOptimizationService.WEBP_EXTENSION, FilenameUtils.getExtension(result.getOptimizedFile().getName()));
				assertTrue(errorMsg, result.isBrowserSpecific());
				assertTrue(errorMsg, result.isFileTypeChanged());
			} else if(imageOptimizationTestDTO.isFileTypeChanged()) {
				assertEquals(errorMsg, FilenameUtils.removeExtension(imageOptimizationTestDTO.getMasterFile().getName()), FilenameUtils.removeExtension(result.getOptimizedFile().getName()));
				assertEquals(errorMsg, IImageOptimizationService.GIF_EXTENSION, FilenameUtils.getExtension(imageOptimizationTestDTO.getMasterFile().getName()));
				assertEquals(errorMsg, IImageOptimizationService.PNG_EXTENSION, FilenameUtils.getExtension(result.getOptimizedFile().getName()));
				assertTrue(errorMsg, result.isFileTypeChanged());
			} else {
				assertEquals(errorMsg, imageOptimizationTestDTO.getMasterFile().getName(), result.getOptimizedFile().getName());
				assertFalse(errorMsg, result.isBrowserSpecific());
				assertFalse(errorMsg, result.isFileTypeChanged());
			}
			
			assertEquals(errorMsg, result.getOptimizedFile().length(), result.getOptimizedFileSize());
			assertNotNull(errorMsg, result.getOriginalFile());
			assertTrue(errorMsg, result.getOriginalFile().exists());
			assertTrue(errorMsg, imageOptimizationTestDTO.getMasterFile().getCanonicalFile().equals(result.getOriginalFile()));
			assertEquals(errorMsg, imageOptimizationTestDTO.getMasterFile().length(), result.getOriginalFileSize());
			
			//The assert is flappy for animated gifs.
			if(!imageOptimizationTestDTO.isAnimatedGif()) {
				assertTrue(errorMsg, imageOptimizationTestDTO.isFailedAutomatedTest() == result.isFailedAutomatedTest());
			}
			assertTrue(errorMsg, result.isOptimized());
		} else {
			assertNull(errorMsg, result);
		}
	}
	
	private static final File getTempDir() throws IOException {
		final File tmpDir = File.createTempFile(ImageOptimizationServiceTest.class.getName(), "");
		tmpDir.delete();
		tmpDir.mkdir();
		tmpDir.deleteOnExit();
		return tmpDir;
	}
	
	private static final int getNumberOfWebPCompatibleImages(final ImageOptimizationTestDTO[] imageOptimizationTestDTOList) {
		int count = 0;
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			if(!imageOptimizationTestDTO.isAnimatedGif() && !imageOptimizationTestDTO.isJPEG()) {
				count++;
			}
		}
		return count;
	}
	
	private static final int getNumberOfOptimizedImages(final ImageOptimizationTestDTO[] imageOptimizationTestDTOList) {
		int count = 0;
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			if(imageOptimizationTestDTO.isOptimized()) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Test for 
	 * {@link ImageOptimizationService#optimizeAllImages(FileTypeConversion, boolean, java.util.Collection)}.
	 * 
	 * @throws IOException can be thrown by the 
	 *                     <code>ImageOptimizationService</code> constructor or 
	 *                     when creating a temp file used in the test
	 * @throws IOException Thrown if there is an issue reading from the file 
	 *                     system.
	 */
	@Test
	public void testOptimizeAllImagesALL() throws IOException {
		
		final ImageOptimizationTestDTO[] imageOptimizationTestDTOList = {new ImageOptimizationTestDTO("csv_120.png", false, false, true),
				                                                         new ImageOptimizationTestDTO("sharing_model2.jpg", false, false, true),
				                                                         new ImageOptimizationTestDTO("loading.gif", false, false, true),
				                                                         new ImageOptimizationTestDTO("el_icon.gif", false, true, true),
				                                                         new ImageOptimizationTestDTO("safe32.png", false, false, true),
				                                                         new ImageOptimizationTestDTO("no_transparency.gif", false, true, true),
				                                                         new ImageOptimizationTestDTO("doctype_16_sprite.png", false, false, false),
				                                                         new ImageOptimizationTestDTO("addCol.gif", false, true, true),
				                                                         new ImageOptimizationTestDTO("s-arrow-bo.gif", false, true, true)};
		
		final int numberOfOptimizedImages = getNumberOfOptimizedImages(imageOptimizationTestDTOList);
		
		final List<File> filesToOptimize = new ArrayList<>(imageOptimizationTestDTOList.length);
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			filesToOptimize.add(imageOptimizationTestDTO.getMasterFile());
		}
		
		//Testing with ALL and no WebP
		List<OptimizationResult<Object>> results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, false, filesToOptimize);
		assertNotNull(results);
		
		Map<String, OptimizationResult<Object>> treasureMap = new HashMap<>(numberOfOptimizedImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			treasureMap.put(result.getOriginalFile().getName(), result);
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfOptimizedImages, results.size());
		assertEquals(numberOfOptimizedImages, treasureMap.size());
		
		//Testing with ALL and YES WebP
		final int numberOfResultImages = numberOfOptimizedImages + getNumberOfWebPCompatibleImages(imageOptimizationTestDTOList);
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, true, filesToOptimize);
		assertNotNull(results);
		
		treasureMap = new HashMap<>(numberOfResultImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			if(FilenameUtils.isExtension(result.getOptimizedFile().getName(), IImageOptimizationService.WEBP_EXTENSION)) {
				treasureMap.put(result.getOriginalFile().getName() + WEBP_ID, result);
			} else {
				treasureMap.put(result.getOriginalFile().getName(), result);
			}
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfResultImages, results.size());
		assertEquals(numberOfResultImages, treasureMap.size());
		
		//WebP Check
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			if(imageOptimizationTestDTO.isJPEG()) {
				//JPEG is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else if(imageOptimizationTestDTO.isAnimatedGif()) {
				//Animated GIF is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else {
				validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID), imageOptimizationTestDTO, true);
			}
		}
		
		//Testing a null list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, false, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, true, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		//Testing an empty list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, false, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.ALL, true, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	/**
	 * Test for 
	 * {@link ImageOptimizationService#optimizeAllImages(FileTypeConversion, boolean, java.util.Collection)}.
	 * 
	 * @throws IOException can be thrown by the 
	 *                     <code>ImageOptimizationService</code> constructor or 
	 *                     when creating a temp file used in the test.
	 * @throws IOException Thrown if there is an issue reading from the file 
	 *                     system.
	 */
	@Test
	public void testOptimizeAllImagesNONE() throws IOException {
		
		final ImageOptimizationTestDTO[] imageOptimizationTestDTOList = {new ImageOptimizationTestDTO("csv_120.png", false, false, true),
                new ImageOptimizationTestDTO("sharing_model2.jpg", false, false, true),
                new ImageOptimizationTestDTO("loading.gif", false, false, true),
                new ImageOptimizationTestDTO("el_icon.gif", false, false, true),
                new ImageOptimizationTestDTO("safe32.png", false, false, true),
                new ImageOptimizationTestDTO("no_transparency.gif", false, false, true),
                new ImageOptimizationTestDTO("doctype_16_sprite.png", false, false, false),
                new ImageOptimizationTestDTO("addCol.gif", false, false, false),
                new ImageOptimizationTestDTO("s-arrow-bo.gif", false, false, true)};

		final int numberOfOptimizedImages = getNumberOfOptimizedImages(imageOptimizationTestDTOList);
		
		final List<File> filesToOptimize = new ArrayList<>(imageOptimizationTestDTOList.length);
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			filesToOptimize.add(imageOptimizationTestDTO.getMasterFile());
		}
		
		//Testing with NONE
		List<OptimizationResult<Object>> results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, false, filesToOptimize);
		assertNotNull(results);
		
		Map<String, OptimizationResult<Object>> treasureMap = new HashMap<>(numberOfOptimizedImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			treasureMap.put(result.getOriginalFile().getName(), result);
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfOptimizedImages, treasureMap.size());
		assertEquals(numberOfOptimizedImages, results.size());
		
		//Testing with NONE and YES WebP
		final int numberOfResultImages = numberOfOptimizedImages + getNumberOfWebPCompatibleImages(imageOptimizationTestDTOList);
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, true, filesToOptimize);
		assertNotNull(results);
		
		treasureMap = new HashMap<>(numberOfResultImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			if(FilenameUtils.isExtension(result.getOptimizedFile().getName(), IImageOptimizationService.WEBP_EXTENSION)) {
				treasureMap.put(result.getOriginalFile().getName() + WEBP_ID, result);
			} else {
				treasureMap.put(result.getOriginalFile().getName(), result);
			}
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfResultImages, treasureMap.size());
		assertEquals(numberOfResultImages, results.size());
		
		//WebP Check
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			if(imageOptimizationTestDTO.isJPEG()) {
				//JPEG is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else if(imageOptimizationTestDTO.isAnimatedGif()) {
				//Animated GIF is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else {
				validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID), imageOptimizationTestDTO, true);
			}
		}
		
		//Testing a null list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, false, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, true, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		//Testing an empty list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, false, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.NONE, true, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	/**
	 * Test for 
	 * {@link ImageOptimizationService#optimizeAllImages(FileTypeConversion, boolean, java.util.Collection)}.
	 * 
	 * @throws IOException can be thrown by the 
	 *                     <code>ImageOptimizationService</code> constructor or 
	 *                     when creating a temp file used in the test.
	 * @throws IOException Thrown if there is an issue reading from the file 
	 *                     system.
	 */
	@Test
	public void testOptimizeAllImagesIE6SAFE() throws IOException {
		
		final ImageOptimizationTestDTO[] imageOptimizationTestDTOList = {new ImageOptimizationTestDTO("csv_120.png", false, false, true),
                new ImageOptimizationTestDTO("sharing_model2.jpg", false, false, true),
                new ImageOptimizationTestDTO("loading.gif", false, false, true),
                new ImageOptimizationTestDTO("el_icon.gif", false, false, true),
                new ImageOptimizationTestDTO("safe32.png", false, false, true),
                new ImageOptimizationTestDTO("no_transparency.gif", false, true, true),
                new ImageOptimizationTestDTO("doctype_16_sprite.png", false, false, false),
                new ImageOptimizationTestDTO("addCol.gif", false, false, false),
                new ImageOptimizationTestDTO("s-arrow-bo.gif", false, false, true)};

		final int numberOfOptimizedImages = getNumberOfOptimizedImages(imageOptimizationTestDTOList);
		
		final List<File> filesToOptimize = new ArrayList<>(imageOptimizationTestDTOList.length);
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			filesToOptimize.add(imageOptimizationTestDTO.getMasterFile());
		}
		
		//Testing with IE6SAFE
		List<OptimizationResult<Object>> results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, false, filesToOptimize);
		assertNotNull(results);
		
		Map<String, OptimizationResult<Object>> treasureMap = new HashMap<>(numberOfOptimizedImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			treasureMap.put(result.getOriginalFile().getName(), result);
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfOptimizedImages, treasureMap.size());
		assertEquals(numberOfOptimizedImages, results.size());
		
		//Testing with IE6SAFE and YES WebP
		final int numberOfResultImages = numberOfOptimizedImages + getNumberOfWebPCompatibleImages(imageOptimizationTestDTOList);
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, true, filesToOptimize);
		assertNotNull(results);
		
		treasureMap = new HashMap<>(numberOfResultImages);
		for(final OptimizationResult<Object> result : results) {
			assertNotNull(result);
			if(FilenameUtils.isExtension(result.getOptimizedFile().getName(), IImageOptimizationService.WEBP_EXTENSION)) {
				treasureMap.put(result.getOriginalFile().getName() + WEBP_ID, result);
			} else {
				treasureMap.put(result.getOriginalFile().getName(), result);
			}
		}
		
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName()), imageOptimizationTestDTO, false);
		}
		assertEquals(numberOfResultImages, treasureMap.size());
		assertEquals(numberOfResultImages, results.size());
		
		//WebP Check
		for(final ImageOptimizationTestDTO imageOptimizationTestDTO : imageOptimizationTestDTOList) {
			if(imageOptimizationTestDTO.isJPEG()) {
				//JPEG is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else if(imageOptimizationTestDTO.isAnimatedGif()) {
				//Animated GIF is not converted to WEBP
				assertNull(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID));
			} else {
				validateFileOptimization(treasureMap.get(imageOptimizationTestDTO.getMasterFile().getName() + WEBP_ID), imageOptimizationTestDTO, true);
			}
		}
		
		//Testing a null list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, false, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, true, (Collection<File>)null);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		//Testing an empty list of images
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, false, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
		results = new ImageOptimizationService<>(getTempDir(), new File(DEFAULT_BINARY_APP_LOCATION)).optimizeAllImages(FileTypeConversion.IE6SAFE, true, Collections.EMPTY_LIST);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	/**
	 * Test method for 
	 * {@link ImageOptimizationService#executeAdvpng(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteAdvpng() throws IOException, InterruptedException {
		
		//Test 1
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "owner_key_icon.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/owner_key_icon.png"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executeAdvpng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
		
		//Test 2
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "csv_120.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/csv_120.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeAdvpng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 3
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "doctype_16_sprite.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/doctype_16_sprite.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeAdvpng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
		
		//Test 4
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sprite arrow enlarge max min shrink x blue.gif.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sprite arrow enlarge max min shrink x blue.gif.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeAdvpng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
	}

	/**
	 * Test method for 
	 * {@link ImageOptimizationService#executePngout(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecutePngout() throws IOException, InterruptedException {
		
		//Test 1
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "owner_key_icon.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/owner_key_icon.png"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executePngout(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
		
		//Test 2
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "csv_120.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/csv_120.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executePngout(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 3
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "doctype_16_sprite.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/doctype_16_sprite.png"), workingFile);
		workingFileSize = workingFile.length();
		
		try {
			optimizedFile = imageOptimizationService.executePngout(workingFile, workingFile.getCanonicalPath());
			fail("Exepected a RuntimeException");
		} catch(final ImageFileOptimizationException ifoe) {
			assertEquals("Error while optimizing the file \"" + workingFile.getCanonicalPath() + "\"", ifoe.getMessage());
		}
		
		//Test 4
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sprite arrow enlarge max min shrink x blue.gif.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sprite arrow enlarge max min shrink x blue.gif.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executePngout(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
	}
	
	private final void testExecuteCWebpHelper(final File fileToConvert) throws IOException, InterruptedException {
		
		final File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + fileToConvert.getName());
		
		FixedFileUtils.copyFile(fileToConvert, workingFile);
		final long workingFileSize = workingFile.length();
		
		final File optimizedFile = imageOptimizationService.executeCWebp(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		if(IImageOptimizationService.JPEG_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(fileToConvert.getName()))) {
			assertTrue(optimizedFile.length() > workingFileSize);
		} else {
			assertTrue(workingFileSize > optimizedFile.length());
		}
		assertEquals(IImageOptimizationService.WEBP_EXTENSION, FilenameUtils.getExtension(optimizedFile.getName()));
	}
	
	/**
	 * Test method for 
	 * {@link ImageOptimizationService#executeCWebp(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteCWebp() throws IOException, InterruptedException {
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/owner_key_icon.png"));
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/csv_120.png"));
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/doctype_16_sprite.png"));
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/safe32.png"));
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sharing_model2.jpg"));
		testExecuteCWebpHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sprite arrow enlarge max min shrink x blue.gif.png"));
	}
	
	private final void testExecuteGif2WebHelper(final File fileToConvert) throws IOException, InterruptedException {
		
		final File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + fileToConvert.getName());
		
		FixedFileUtils.copyFile(fileToConvert, workingFile);
		final long workingFileSize = workingFile.length();
		
		final File optimizedFile = imageOptimizationService.executeGif2Webp(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		assertEquals(IImageOptimizationService.WEBP_EXTENSION, FilenameUtils.getExtension(optimizedFile.getName()));
	}
	
	/**
	 * Test method for 
	 * {@link ImageOptimizationService#executeGif2Webp(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteGif2Web() throws IOException, InterruptedException {
		testExecuteGif2WebHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/el_icon.gif"));
		testExecuteGif2WebHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/loading.gif"));
		testExecuteGif2WebHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/no_transparency.gif"));
		testExecuteGif2WebHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/addCol.gif"));
		testExecuteGif2WebHelper(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/s arrow bo.gif"));
	}

	/**
	 * Test method for 
	 * {@link ImageOptimizationService#executeOptipng(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteOptipng() throws IOException, InterruptedException {
		
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "owner_key_icon.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/owner_key_icon.png"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executeOptipng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
		
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "csv_120.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/csv_120.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeOptipng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "doctype_16_sprite.png");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/doctype_16_sprite.png"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeOptipng(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
	}

	/**
	 * Test for {@link ImageOptimizationService#executeJpegtran(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteJpegtran() throws IOException, InterruptedException {
		
		//Test 1
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sharing_model2.jpg");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sharing_model2.jpg"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executeJpegtran(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
		
		//Test 2
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sharin g model2.jpg");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sharin g model2.jpg"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeJpegtran(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertEquals(workingFileSize, optimizedFile.length());
	}

	/**
	 * Test for 
	 * {@link ImageOptimizationService#executeJfifremove(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteJfifremove() throws IOException, InterruptedException {
		
		//Test 1
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sharing_model2.jpg");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sharing_model2.jpg"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executeJfifremove(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 2
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "sharin g model2.jpg");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/sharin g model2.jpg"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeJfifremove(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
	}

	/**
	 * Test for {@link ImageOptimizationService#executeGifsicle(File, String)}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 * @throws InterruptedException Can be thrown by the optimization service 
	 *                              when optimizing the files.
	 */
	@Test
	public void testExecuteGifsicle() throws IOException, InterruptedException {
		//Test 1
		File workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "el_icon.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/el_icon.gif"), workingFile);
		long workingFileSize = workingFile.length();
		
		File optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 2
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "loading.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/loading.gif"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 3
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "no_transparency.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/no_transparency.gif"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 4
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "addCol.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/addCol.gif"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize == optimizedFile.length());
		
		//Test 5
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "s-arrow-bo.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/s-arrow-bo.gif"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
		
		//Test 6
		workingFile = new File(getTempDir().getCanonicalFile() + File.separator + "s arrow bo.gif");
		
		FixedFileUtils.copyFile(new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service/s arrow bo.gif"), workingFile);
		workingFileSize = workingFile.length();
		
		optimizedFile = imageOptimizationService.executeGifsicle(workingFile, workingFile.getCanonicalPath());
		assertNotNull(optimizedFile);
		assertTrue(optimizedFile.exists());
		assertTrue(workingFileSize > optimizedFile.length());
	}

	/**
	 * Test for {@link ImageOptimizationService#getFinalResultsDirectory()}.
	 * 
	 * @throws IOException Can be thrown when interacting with various files.
	 */
	@Test
	public void testGetFinalResultsDirectory() throws IOException {
		final File tmpDir = getTempDir();
		
		assertEquals(tmpDir.getCanonicalPath() + File.separator + "final", (new ImageOptimizationService<>(tmpDir, new File(DEFAULT_BINARY_APP_LOCATION))).getFinalResultsDirectory());
	}
	
	private static class ImageOptimizationTestDTO {
		
		private final File masterFile;
		private final long masterFileChecksum;
		private final boolean failedAutomatedTest;
		private final boolean fileTypeChanged;
		private final boolean isJPEG;
		private final boolean isAnimatedGif;
		private final boolean isOptimized;
		
		/**
		 * @param fileName The name of the file being tested.
		 * @param failedAutomatedTest Used to indicate if a failed automated 
		 *                            validation is expected.
		 * @param fileTypeChanged Used to indicate if a file type change is 
		 *                        expected.
		 * @param isOptimized Used to indicate if the image is expected to be 
		 *                    optimized.
		 * @throws IOException Thrown when calculating the masterFileChecksum
		 */
		ImageOptimizationTestDTO(final String fileName, final boolean failedAutomatedTest, final boolean fileTypeChanged, final boolean isOptimized) throws IOException {
			masterFile = new File("./test/com/salesforce/perfeng/uiperf/imageoptimization/service" + File.separator + fileName);
			assertTrue(masterFile.exists());
			masterFileChecksum = FileUtils.checksumCRC32(masterFile);
			this.failedAutomatedTest = failedAutomatedTest;
			this.fileTypeChanged = fileTypeChanged;
			this.isJPEG = (IImageOptimizationService.JPEG_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(fileName)));
			this.isAnimatedGif = ImageUtils.isAminatedGif(masterFile);
			this.isOptimized = isOptimized;
		}
		
		public File getMasterFile() {
			return masterFile;
		}
		public long getMasterFileChecksum() {
			return masterFileChecksum;
		}
		public boolean isFailedAutomatedTest() {
			return failedAutomatedTest;
		}
		public boolean isFileTypeChanged() {
			return fileTypeChanged;
		}
		public boolean isJPEG() {
			return isJPEG;
		}
		public boolean isAnimatedGif() {
			return isAnimatedGif;
		}
		public boolean isOptimized() {
			return isOptimized;
		}
	}
}