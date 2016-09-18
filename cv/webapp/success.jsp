<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="true"%>
<!DOCTYPE html>
<html>
<head>
<title>交汇教育</title>
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width,initial-scale=1,user-scalable=0">
<link rel="stylesheet"
	href="https://res.wx.qq.com/open/libs/weui/0.4.2/weui.css" />
<link rel="stylesheet" href="./resources/css/style.css" />
</head>

<body>
	<p>绑定成功！</p>
	<p>您的会员号是：${number} </p>
	<p><a href="./${number}">点击查看</a></p>
</body>
</html>