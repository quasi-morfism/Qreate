package com.morfism.aiappgenerator.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @PreDestroy // Close before backend shutdown
    public void destroy() {
        webDriver.quit();
    }

    /**
     * Initialize Chrome browser driver
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // Auto-manage ChromeDriver
            WebDriverManager.chromedriver().setup();
            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            // Headless mode
            options.addArguments("--headless");
            // Disable GPU (avoid issues in some environments)
            options.addArguments("--disable-gpu");
            // Disable sandbox mode (required for Docker environment)
            options.addArguments("--no-sandbox");
            // Disable dev shm usage
            options.addArguments("--disable-dev-shm-usage");
            // Set window size
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // Disable extensions
            options.addArguments("--disable-extensions");
            // Set user agent
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // Create driver
            WebDriver driver = new ChromeDriver(options);
            // Set page load timeout
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // Set implicit wait
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("Failed to initialize Chrome browser", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to initialize Chrome browser");
        }




    }
    /**
     * Save image to file
     */
    private static void saveImage(byte[] imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("Failed to save image: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to save image");
        }
    }
    /**
     * Compress image
     */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        // Compress image quality (0.1 = 10% quality)
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("Failed to compress image: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to compress image");
        }
    }

    /**
     * Wait for page load completion
     */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            // Create page load wait object
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            // Wait for document.readyState to be complete
            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState")
                            .equals("complete")
            );
            // Additional wait to ensure dynamic content is loaded
            Thread.sleep(2000);
            log.info("Page load completed");
        } catch (Exception e) {
            log.error("Exception occurred while waiting for page load, continuing with screenshot", e);
        }
    }

    /**
     * Generate web page screenshot
     *
     * @param webUrl Web page URL
     * @return Compressed screenshot file path, returns null on failure
     */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("Web page URL cannot be empty");
            return null;
        }
        try {
            // Create temporary directory
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);
            // Image suffix
            final String IMAGE_SUFFIX = ".png";
            // Original screenshot file path
            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;
            // Visit web page
           webDriver.get(webUrl);
            // Wait for page load completion
            waitForPageLoad(webDriver);
            // Take screenshot
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            // Save original image
            saveImage(screenshotBytes, imageSavePath);
            log.info("Original screenshot saved successfully: {}", imageSavePath);
            // Compress image
            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("Compressed image saved successfully: {}", compressedImagePath);
            // Delete original image, keep only compressed image
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("Web page screenshot failed: {}", webUrl, e);
            return null;
        }
    }






}
