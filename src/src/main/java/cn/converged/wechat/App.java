package cn.converged.wechat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * WeChat App for converged.cn
 *
 */
public class App {

	// ~ 静态成员 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 静态方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 成员变量 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	private String appName = "/cv";

	private Logger logger = Logger.getLogger(App.class.getName());
	private String mWorkingDir = System.getProperty("java.io.tmpdir");
	private Integer port = 18080;
	private Tomcat tomcat = null;	
	String dataPath = "";

	private String webRoot = "/src/main/webapp";
	private boolean isDev = true;

	// ~ 构造方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	public App() {
	}

	private String projectRoot() {
		File file = new File("./");
		return file.getAbsolutePath();
	}

	public void run() {
		tomcat = new Tomcat();
		tomcat.setPort(port);
		tomcat.setBaseDir(mWorkingDir);
		tomcat.getHost().setAppBase(mWorkingDir);
		tomcat.getHost().setAutoDeploy(true);
		tomcat.getHost().setDeployOnStartup(true);

		// 当前目录
		String projectRoot = projectRoot();
		// 部署目录或WAR
		Context appContext = tomcat.addWebapp( //
				tomcat.getHost(), appName, // 映射地址
				projectRoot + webRoot// 磁盘路径
		);
		logger.info(appContext.getBaseName() //
				+ " 部署为 " + appContext.getName());

		// 扫描和添加Servlet配置
		if (isDev) { // - 开发环境
			File additionWebInfClasses = new File("target/classes");
			if (additionWebInfClasses.exists()) {
				WebResourceRoot webResources = new StandardRoot(appContext);
				webResources.addPreResources( //
						new DirResourceSet( //
								webResources, "/WEB-INF/classes", //
								additionWebInfClasses.getAbsolutePath(), "/"));
				appContext.setResources(webResources);
			}
		}

		try {
			tomcat.start();
		} catch (LifecycleException e) {
			logger.severe("服务器无法启动！");
			e.printStackTrace();
		}
		logger.info("服务器成功启动 => " //
				+ tomcat.getHost() + ":" + port);

		tomcat.getServer().await();
	}

	// ~ 成员方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	/** 解压缩JAR文件 */
	private void uncompress(String filename) {
		final int BUFFER = 2048;
		try {
			FileInputStream fis = new FileInputStream(filename);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry = null;
			while ((entry = zis.getNextEntry()) != null) {
				System.out.println("解压缩文件：" + entry);
				int count;
				byte data[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new FileOutputStream(entry.getName());
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** 读取配置文件 */
	private void config() {
		Properties properties = new Properties();
		String configPath = projectRoot() + "/config.properties";
		if (isDev) {
			configPath = projectRoot() + "/src/main/webapp/config.properties";
		}
		File configFile = new File(configPath);
		try {
			properties.load(new FileInputStream(configFile));
			String config = properties.getProperty("port", "18080");
			int port = NumberUtils.toInt(config, this.port);
			System.out.println("端口号：" + port);
			this.port = port;
			String dataPath = properties.getProperty("data", "");
			System.out.println("数据目录：" + dataPath);
			this.dataPath = dataPath;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ~ g^setX ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ main() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	public static void main(String[] args) {
		boolean isDev = false;
		// 取命令行参数
		if (args != null && args.length > 0) {
			if ("isDev".equals(args[0])) {
				isDev = true;
			}
		}
		App main = new App();
		if (!isDev) { // 生产环境
			main.webRoot = "/webapp";
			main.isDev = false;
		}
		boolean isSingleJar = false;
		if (isSingleJar) {
			main.uncompress(""); // 解压缩
		}
		main.config(); // 配置环境
		main.run(); // 启动服务器

		// 测试服务器启动是否成功

	}
}
