<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page session="true"%>
<!DOCTYPE html>
<!--显示：会员号（数字），剩余次数，有效日期，学费折扣，奖励次数-->
<html>
<head>
<title>会员卡</title>
<meta charset="utf-8">
<meta name="viewport"
	content="width=device-width,initial-scale=1,user-scalable=0">
<link rel="stylesheet"
	href="https://res.wx.qq.com/open/libs/weui/0.4.2/weui.css" />
<link rel="stylesheet" href="../resources/css/style.css" />
</head>

<body>
	<div class="container">
		<!--页面头部-->
		<div class="index_head">
			<div class="head_img">
				交汇教育 <br /> 青少年双语教育
			</div>
			<div class="page_hd">
				<h2 class="page_title">会员卡</h2>
				<p>这里可以查看你的会员卡信息</p>
			</div>
		</div>

		<!--页面内容-->
		<div class="weui_cells_title">会员卡信息</div>
		<div class="weui_cells">
			<div class="weui_cell">
				<div class="weui_cell_hd weui">
					<img class="icon" src="../resources/images/icon_nav_panel.png" />
				</div>
				<div class="weui_cell_bd weui_cell_primary">
					<p>会员号</p>
				</div>
				<div class="weui_cell_ft">${memberInfo.number}</div>
			</div>

			<div class="weui_cell">
				<div class="weui_cell_hd weui">
					<img class="icon" src="../resources/images/icon_nav_actionSheet.png" />
				</div>
				<div class="weui_cell_bd weui_cell_primary">
					<p>剩余次数</p>
				</div>
				<div class="weui_cell_ft">${memberInfo.times}</div>
			</div>

			<div class="weui_cell">
				<div class="weui_cell_hd weui">
					<img class="icon" src="../resources/images/icon_nav_icons.png" />
				</div>
				<div class="weui_cell_bd weui_cell_primary">
					<p>有效日期</p>
				</div>
				<div class="weui_cell_ft">${memberInfo.expired}</div>
			</div>

			<div class="weui_cell">
				<div class="weui_cell_hd weui">
					<img class="icon" src="../resources/images/icon_nav_progress.png" />
				</div>
				<div class="weui_cell_bd weui_cell_primary">
					<p>学费折扣</p>
				</div>
				<div class="weui_cell_ft">${memberInfo.discount}</div>
			</div>

			<div class="weui_cell">
				<div class="weui_cell_hd weui">
					<img class="icon" src="../resources/images/icon_nav_toast.png" />
				</div>
				<div class="weui_cell_bd weui_cell_primary">
					<p>奖励次数</p>
				</div>
				<div class="weui_cell_ft">${memberInfo.award}</div>
			</div>
		</div>

		<!--按钮-->
		<div class="btn">
			<a href="../index.html" class="weui_btn weui_btn_primary">返回</a>
		</div>
	</div>
</body>
</html>