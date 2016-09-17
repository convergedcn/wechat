/**
 * 
 */
package cn.converged.wechat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
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
@MultipartConfig( //
location = "/tmp", //
fileSizeThreshold = 1024 * 1024, //
maxFileSize = 1024 * 1024 * 50, //
maxRequestSize = 1024 * 1024 * 100)
public class WeChatServlet extends HttpServlet {

	// ~ 静态成员 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	private static final long serialVersionUID = 1L;

	// ~ 静态方法 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	// ~ 成员变量 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ //

	Logger logger = LogManager.getLogger(getClass());

	private static final String TOKEN = "weixin";

	String rootPath = null;

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
			// 检查用户登录信息
			HttpSession session = request.getSession(true);
			Object openId = session.getAttribute("openId");
			if (openId != null) { // 已登录
				response.sendRedirect("../info.html");
			} else { // 未登录 - 提示关注|注册
				response.sendRedirect("../index.html");
			}
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
	@SuppressWarnings("unchecked")
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) //
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		Object result = null;
		// 读取服务器路径
		String rootPath = request.getServletContext().getRealPath("");
		this.rootPath = rootPath;
		// 读取微信消息
		Element root = readXml(request);
		if (root != null) { // 是XML消息
			// String myId = root.elementTextTrim("ToUserName");
			// TODO 检查是否正确
			// if (!myId.equals("abc")) {
			// logger.error("不对！");
			// }
			String openId = root.elementTextTrim("FromUserName");
			// 检查消息类型
			String xmlMsgType = root.elementTextTrim("MsgType");
			if ("event".equals(xmlMsgType)) { // 处理事件消息
				String xmlEvent = root.elementTextTrim("Event");
				xmlEvent = xmlEvent.toLowerCase();
				if ("click".equals(xmlEvent)) { // 用户点击自定义菜单
					String xmlEventKey = root.elementTextTrim("EventKey");
					if ("member".equals(xmlEventKey)) { // 查看会员信息
						Map<String, Object> member = null;
						// 先检查Session
						HttpSession session = request.getSession(true);
						Object openIdSession = session.getAttribute("openId");
						if (openId.equals(openIdSession)) { // 已登录
							member = (Map<String, Object>) session.getAttribute("member");
							result = "http://wechat.converged.cn/info.html";
						} else { // 未登录
							member = readMember(openId);
							if (member != null) { // 保存到Session（登录）
								session.setAttribute("openId", openId);
								session.setAttribute("member", member);
								result = "http://wechat.converged.cn/info.html";
							} else { // 非会员，清除（可能换用户登录了）
								session.setAttribute("openId", null);
								session.setAttribute("member", null);
								result = "http://wechat.converged.cn/signin.html";
							}
						}
					}
				}
			} else { // 其他消息
				// TODO
			}
		} else { // 不是XML，可能是直接访问
			// 检查Session，要求必须从微信发起访问
			HttpSession session = request.getSession(true);
			Object openIdSession = session.getAttribute("openId");
			if (openIdSession != null) { // 已登录
				// 返回信息
				Object info = session.getAttribute("member");
				result = JsonUtils.toString(info);
			} else { // 未登录
				// 提示只能通过微信访问
				result = "请通过微信访问！";
			}
		}

		// 返回信息
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

	/** 读取会员数据 */
	private Map<String, Object> readMember(String openId) {
		String memberPath = this.rootPath + "/member/";
		String memberFile = memberPath + openId + ".json";
		File jsonFile = new File(memberFile);
		if (jsonFile.exists() && jsonFile.isFile()) {
			try {
				FileReader fr = new FileReader(jsonFile);
				BufferedReader br = new BufferedReader(fr);
				StringBuilder jsonText = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					jsonText.append(line);
					line = br.readLine();
				}
				br.close();
				fr.close();
				// 返回文件中的所有数据
				return JsonUtils.toMap(jsonText.toString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 文件不存在，不存在这个会员
		return null;
	}

	private void recordAccess(Element root) {
		String openId = root.elementTextTrim("FromUserName");
		String accessPath = this.rootPath + "/access/";
		Date now = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timestamp = sdf.format(now);
		String filename = accessPath + openId + timestamp + ".xml";
		try {
			FileWriter fw = new FileWriter(new File(filename));
			fw.write(root.getDocument().asXML());
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

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
