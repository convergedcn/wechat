/**
 * 
 */
package cn.converged.wechat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.zengsource.utils.JsonUtils;

/**
 * Member Services.
 * 
 * @author zengsn
 * @since 8.0
 */
@WebServlet(urlPatterns = { "/member/*" })
// @MultipartConfig( //
// location = "/tmp", //
// fileSizeThreshold = 1024 * 1024, //
// maxFileSize = 1024 * 1024 * 50, //
// maxRequestSize = 1024 * 1024 * 100)
public class WeChatServlet extends HttpServlet {

	// ~ 静态成员 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	private static final long serialVersionUID = 1L;

	public static final String URL_INDEX = "/member/index";
	public static final String URL_SIGNUP = "/member/signup";

	public static Map<Integer, String> memberMap = null;
	public static Map<String, Object> messageMap = null;

	// ~ 静态方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 成员变量 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	Logger logger = LogManager.getLogger(getClass());

	private static final String TOKEN = "weixin";

	String rootPath = null;
	String myOpenId = null;

	// ~ 构造方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	public WeChatServlet() {
	}

	// ~ 成员方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	String getString(HttpServletRequest request, String name) {
		String param = request.getParameter(name);
		return param == null ? "" : param;
	}

	/** 连接微信公众号 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) //
			throws ServletException, IOException {
		// 读取服务器路径
		String rootPath = request.getServletContext().getRealPath("");
		this.rootPath = rootPath;
		// 读取参数
		String signature = getString(request, "signature");
		String timestamp = getString(request, "timestamp");
		String nonce = getString(request, "nonce");
		String echostr = getString(request, "echostr");
		if (signature.equals(buildSignature(TOKEN, timestamp, nonce))) {
			logger.info("==> Weixin signature \n" + echostr);
			try {
				response.getWriter().print(echostr);
				response.getWriter().flush();
				response.getWriter().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else { // 其他GET访问，显示公众号二维码
			String forwardPage = null;
			String url = request.getRequestURL().toString();
			if (url.contains(URL_INDEX)) { // index
			} else if (url.contains(URL_SIGNUP)) {
				int pos = url.indexOf(URL_SIGNUP);
				String singupToken = url.substring(pos + URL_SIGNUP.length());
				request.setAttribute("token", singupToken); // 注册号
				forwardPage = "/signup.jsp";
			} else { // 任意访问
				int pos = url.indexOf("/member/");
				String resource = url.substring(pos + 8);
				int number = NumberUtils.toInt(resource, 0);
				if (number > 0) { // 必须指定会员号
					Map<?, ?> memberInfo = readMember(number);
					request.setAttribute("memberInfo", memberInfo);
					forwardPage = "/info.jsp";
				} else { // 未指定会员号
					forwardPage = "/index.html";
				}
			}
			// 跳转 JSP 页面
			RequestDispatcher dispatcher = request //
					.getRequestDispatcher(forwardPage);
			dispatcher.forward(request, response);
		}
	}

	private String buildSignature(String token, String timestamp, String nonce) {
		List<String> list = new ArrayList<String>();
		list.add(token);
		list.add(timestamp);
		list.add(nonce);
		Collections.sort(list); // 字符串排序

		StringBuffer sb = new StringBuffer();
		for (String str : list) {
			sb.append(str); // 字符串拼接
		}

		return this.encryptBySHA1(sb.toString()); // SHA加密
	}

	private String encryptBySHA1(String src) {
		byte[] bytes = null;
		StringBuffer sb = new StringBuffer();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(src.getBytes("utf-8"));
			bytes = md.digest();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toHexString((0x000000ff & bytes[i]) | 0xffffff00).substring(6));
			}
		} catch (Exception ex) {
			return null;
		}
		return sb.toString();
	}

	/** 接收微信请求 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) //
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		Object result = null;
		// 读取服务器路径
		String rootPath = request.getServletContext().getRealPath("");
		this.rootPath = rootPath;
		// String tmp1 = getString(request, "token");
		// String tmp2 = getString(request, "name");
		String contentType = request.getHeader("Content-Type");
		if ("application/x-www-form-urlencoded".equals(contentType)) {
			String forwardPage = null;
			String url = request.getRequestURL().toString();
			if (url.contains(URL_INDEX)) { // index
			} else if (url.contains(URL_SIGNUP)) {
				String token = getString(request, "token");
				String openId = findByToken(token);
				if (openId == null) {
					request.setAttribute("error", responseMessage("error_only_wechat"));
					forwardPage = "/error.jsp";
				} else { // 开始注册
					String name = getString(request, "name");
					Map<String, Object> memberInfo = readData(name);
					if (memberInfo == null) { // 没有这个学生
						request.setAttribute("error", responseMessage("error_no_student"));
						forwardPage = "/error.jsp";
					} else { // 创建记录文件
						memberInfo.put("openId", openId);
						String memberPath = rootPath + "/member/" + openId + ".json";
						// 检查该用户是否已经注册过
						File memberFile = new File(memberPath);
						if (memberFile.exists()) { // 文件已存在
							request.setAttribute("error", responseMessage("error_duplicated") //
									+ "（会员号：" + memberInfo.get("number") + "）");
							forwardPage = "/error.jsp";
						} else { // 保存注册记录
							FileWriter fw = new FileWriter(memberFile);
							fw.write(JsonUtils.toString(memberInfo));
							fw.close(); // 写文件结束
							// 修改数据文件
							renameDataFile(name);
							// 返回查看页面
							int number = (int) memberInfo.get("number");
							request.setAttribute("success", true);
							request.setAttribute("number", number);
							forwardPage = "/success.jsp";
						}
					}
				}
			}
			// 跳转 JSP 页面
			RequestDispatcher dispatcher = request //
					.getRequestDispatcher(forwardPage);
			dispatcher.forward(request, response);
		} else { // 其他的是微信的
			// 读取微信消息
			Element root = readXml(request);
			if (root != null) { // 是XML消息
				result = processMessage(root);
			} else { // 返回

			}
			// 返回信息到微信中
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json; charset=utf-8");
			Writer writer = response.getWriter();
			if (result != null) { // JSON
				logger.info("Result >>> \n" + result);
				writer.write(result.toString());
			} else { // 请求格式不正确
				writer.write("Bad Request!");
			}
		}
	}

	/** 处理微信消息 */
	private String processMessage(Element xml) {
		String result = null;
		// ///////////////////////////////////////////////////////////// //
		// 微信消息接收方 ToUserName
		// ///////////////////////////////////////////////////////////// //
		String toUserName = xml.elementTextTrim("ToUserName");
		// 检查是否正确
		if (this.myOpenId != null // ID不对
				&& !toUserName.equals(this.myOpenId)) {
			logger.error("ToUserName 不对：" + toUserName);
			return responseMessage("msg_converged");
		}
		// ///////////////////////////////////////////////////////////// //
		// 微信消息发送方 FromUserName
		// ///////////////////////////////////////////////////////////// //
		String fromUserName = xml.elementTextTrim("FromUserName");
		// ///////////////////////////////////////////////////////////// //
		// 微信消息带有唯一ID MsgId
		// ///////////////////////////////////////////////////////////// //
		String msgId = xml.elementTextTrim("MsgId");
		if (msgId == null) { // 事件型消息没有MsgId，
			// 用 CreateTime 替代
			// //////////////////////////////////////////////////////// //
			// 微信消息带有创建时间 CreateTime
			// //////////////////////////////////////////////////////// //
			msgId = xml.elementTextTrim("CreateTime");
		}
		// TODO 处理消息并发
		// 检查消息类型
		String xmlMsgType = xml.elementTextTrim("MsgType");
		// ////////////////////////////////////////////////////////////////// //
		// 微信传来事件消息 MsgType == event
		// ////////////////////////////////////////////////////////////////// //
		if ("event".equals(xmlMsgType)) { // 处理事件消息
			String xmlEvent = xml.elementTextTrim("Event");
			xmlEvent = xmlEvent.toLowerCase();
			// ///////////////////////////////////////////////////////////// //
			// 有新用户关注公众号 Event == subscribe
			// ///////////////////////////////////////////////////////////// //
			if ("subscribe".equals(xmlEvent)) {
				result = responseMessage("msg_hello") + "\n";
				// 判断该用户之前是否已经关注过
				Object memberInfo = readMember(fromUserName);
				if (memberInfo == null) { // 第一次关注
					result += responseMessage("msg_pls_signup") + msgId;
				} else { // 已关注过
					Map<?, ?> infoMap = (Map<?, ?>) memberInfo;
					Object number = infoMap.get("number");
					result += responseMessage("msg_view_info") + number + "\n";
					result += responseMessage("msg_tip_link") + "\n";
					result += responseMessage("msg_tip_number") + number;
				}
			} else if ("click".equals(xmlEvent)) { // 用户点击自定义菜单
				String xmlEventKey = xml.elementTextTrim("EventKey");
				if ("member".equals(xmlEventKey)) { // 查看会员信息
					// TODO 等公众号认证后再实现
				}
			}
		}
		// ////////////////////////////////////////////////////////////////// //
		// 微信传来文本消息 MsgType == text
		// ////////////////////////////////////////////////////////////////// //
		else if ("text".equals(xmlMsgType)) { // 处理文本消息
			Object memberInfo = readMember(fromUserName);
			if (memberInfo == null) { // 未绑定帐号的用户
				result = responseMessage("msg_pls_signup") + msgId + "\n";
				result += responseMessage("msg_find_us");
			} else { // 已绑定帐号
				Map<?, ?> infoMap = (Map<?, ?>) memberInfo;
				int number = (int) infoMap.get("number");
				// ////////////////////////////////////////////////////////// //
				// 微信文本消息内容 Content
				// ////////////////////////////////////////////////////////// //
				String text = xml.elementTextTrim("Content");
				int textInt = NumberUtils.toInt(text, 0);
				if (textInt == number) { // 用户输入自己的会员号直接查看
					int times = (int) infoMap.get("times");
					double discount = (double) infoMap.get("discount");
					int award = (int) infoMap.get("award");
					String message = responseMessage("msg_member_info");
					result = String.format(message, number, times, discount, award);
					result += responseMessage("msg_view_info") + number + "\n";
				} else { // 返回链接让用户在网页上查看
					result = responseMessage("msg_view_info") + number + "\n";
					result += responseMessage("msg_tip_link") + "\n";
					result += responseMessage("msg_tip_number") + number;
				}
			}
		}
		return textMessage(fromUserName, result);
	}

	/** 读取微信发来的XML消息 */
	public Element readXml(HttpServletRequest request) {
		SAXReader saxReader = new SAXReader();
		try {
			Document doc = saxReader.read(request.getInputStream());
			String xmlIn = (doc == null) ? "" : doc.asXML();
			logger.info("==> Weixin request \n" + xmlIn);
			if (doc != null) { // 记录访问记录
				Element root = doc.getRootElement();
				recordAccess(root);
				return root;
			}
		} catch (DocumentException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return null;
	}

	/** 根据微信OpenId读取会员数据 */
	private Map<String, Object> readMember(String openId) {
		String memberPath = this.rootPath + "/member/";
		String memberFile = memberPath + openId + ".json";
		File jsonFile = new File(memberFile);
		String text = readText(jsonFile);
		// 文件不存在，不存在这个会员
		return text != null ? JsonUtils.toMap(text) : null;
	}

	/** 根据会员号读取会员信息 */
	private Map<?, ?> readMember(int number) {
		if (memberMap == null) {
			memberMap = new HashMap<>();
			File memberDir = new File(this.rootPath + "/member");
			File[] files = memberDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.getName().endsWith(".json");
				}
			});
			// 先读出来缓存
			for (File file : files) {
				String openId = file.getName().replace(".json", "");
				Map<?, ?> memberInfo = readMember(openId);
				if (memberInfo != null) {
					Object tmp = memberInfo.get("number");
					Integer temp = tmp != null ? (int) tmp : 0;
					// 只缓存OpenId
					memberMap.put(temp, openId);
				}
			}
		}
		Integer intNum = number;
		String openId = memberMap.get(intNum);
		return openId != null ? readMember(openId) : null;
	}

	/** 根据学生姓名读取预设数据 */
	private Map<String, Object> readData(String name) {
		String propoeriesPath = this.rootPath + "/config.properties";
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(propoeriesPath));
			String dataPath = properties.getProperty("data", "");
			String dataFile = dataPath + name + ".json";
			String text = readText(new File(dataFile));
			return text != null ? JsonUtils.toMap(text) : null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void renameDataFile(String name) {
		String propoeriesPath = this.rootPath + "/config.properties";
		Properties properties = new Properties();
		try {
			properties.load(new FileReader(propoeriesPath));
			String dataPath = properties.getProperty("data", "");
			String filname1 = dataPath + name + ".json";
			String filname2 = dataPath + name + "~.json";
			File file1 = new File(filname1);
			File file2 = new File(filname2);
			file1.renameTo(file2);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String readText(File file) {
		if (file.exists() && file.isFile()) {
			try {
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				StringBuilder text = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					text.append(line);
					line = br.readLine();
				}
				br.close();
				fr.close();
				return text.toString();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/** 保存微信访问记录 */
	private void recordAccess(Element root) {
		if (this.myOpenId == null) {
			this.myOpenId = root.elementTextTrim("ToUserName");
		}
		String openId = root.elementTextTrim("FromUserName");
		String accessPath = this.rootPath + "/access/";
		String msgId = root.elementTextTrim("MsgId");
		if (msgId == null) { // 关注时（事件）没有MsgId，
			// 用 CreateTime 替代
			msgId = root.elementTextTrim("CreateTime");
		}
		String filename = accessPath + openId + msgId + ".xml";
		try {
			FileWriter fw = new FileWriter(new File(filename));
			fw.write(root.getDocument().asXML());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** 根据微信访问记录找到用户ID，限制只能用微信查看 */
	private String findByToken(String token) {
		File accessDir = new File(this.rootPath + "/access");
		String[] filenames = accessDir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(token + ".xml");
			}
		});
		if (filenames != null && filenames.length > 0) {
			String filename = filenames[0];
			return filename.replace(token + ".xml", "");
		}
		return null;
	}

	private String textMessage(String toUserName, String text) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("xml");
		root.addElement("ToUserName").addCDATA(toUserName);
		root.addElement("FromUserName").addCDATA(this.myOpenId);
		root.addElement("CreateTime").addCDATA(System.currentTimeMillis() + "");
		root.addElement("MsgType").addCDATA("text");
		root.addElement("Content").addCDATA(text);
		return doc.asXML();
	}

	private String responseMessage(String key) {
		if (messageMap == null) {
			messageMap = new HashMap<>();
			String filename = rootPath + "/messages.properties";
			Properties properties = new Properties();
			try {
				properties.load(new FileReader(filename));
				for (Object name : properties.keySet()) {
					messageMap.put(name.toString(), // 加载并缓存
							properties.getProperty(name.toString()));
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Object value = messageMap.get(key);
		return value == null ? null : value.toString();
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) //
			throws ServletException, IOException {
	}

	@Override
	public void doDelete(HttpServletRequest request, HttpServletResponse response) //
			throws ServletException, IOException {
	}

	// ~ g^setX ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ main() ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

}
