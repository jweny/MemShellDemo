<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="s" uri="/struts-tags" %>
<html>
  <head>
    <title>上传图片</title>
  </head>
  <body>
    <s:actionerror/>
  <s:form action="upload.action" enctype="multipart/form-data">
    <s:textfield name="userName"  label="用户名"/>
    <s:file name="file" label="选择图片"></s:file>
    <s:submit value="提交" />
  </s:form>
  </body>
</html>
