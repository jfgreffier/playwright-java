package com.microsoft.playwright.junit.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.impl.Utils;
import com.microsoft.playwright.junit.Options;
import org.junit.jupiter.api.extension.*;

import static com.microsoft.playwright.junit.impl.ExtensionUtils.isParameterSupported;

public class BrowserExtension implements ParameterResolver, AfterAllCallback {
  private static final ThreadLocal<Browser> threadLocalBrowser = new ThreadLocal<>();

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    Browser browser = threadLocalBrowser.get();
    threadLocalBrowser.remove();
    if (browser != null) {
      browser.close();
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return isParameterSupported(parameterContext, extensionContext, Browser.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    return getOrCreateBrowser(extensionContext);
  }

  static Browser getOrCreateBrowser(ExtensionContext extensionContext) {
    Browser browser = threadLocalBrowser.get();
    if (browser != null) {
      return browser;
    }

    Options options = OptionsExtension.getOptions(extensionContext);
    Playwright playwright = PlaywrightExtension.getOrCreatePlaywright(extensionContext);
    BrowserType.LaunchOptions launchOptions = getLaunchOptions(options);

    BrowserType browserType = playwright.chromium();
    if (options.browserName != null) {
      browserType = getBrowserTypeForName(playwright, options.browserName);
    } else if (options.deviceName != null) {
      DeviceDescriptor deviceDescriptor = DeviceDescriptor.findByName(playwright, options.deviceName);
      if (deviceDescriptor != null && deviceDescriptor.defaultBrowserType != null) {
          browserType = getBrowserTypeForName(playwright, deviceDescriptor.defaultBrowserType);
      }
    }
    browser = browserType.launch(launchOptions);

    threadLocalBrowser.set(browser);
    return browser;
  }

  private static BrowserType getBrowserTypeForName(Playwright playwright, String name) {
    switch (name) {
      case "webkit":
        return playwright.webkit();
      case "firefox":
        return playwright.firefox();
      case "chromium":
        return playwright.chromium();
      default:
        throw new PlaywrightException("Invalid browser name.");
    }
  }

  private static BrowserType.LaunchOptions getLaunchOptions(Options options) {
    BrowserType.LaunchOptions launchOptions = Utils.clone(options.launchOptions);
    if (launchOptions == null) {
      launchOptions = new BrowserType.LaunchOptions();
    }

    if (options.headless != null) {
      launchOptions.setHeadless(options.headless);
    }

    if (options.channel != null) {
      launchOptions.setChannel(options.channel);
    }

    return launchOptions;
  }
}
