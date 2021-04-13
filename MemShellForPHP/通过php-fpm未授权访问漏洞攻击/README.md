# 通过php-fpm未授权访问漏洞攻击

## 一、攻击原理

当php-fpm对外开放时，通过构造Fastcgi协议（fastcgi协议是服务器中间件和后端进行数据交换的协议），通过向php-fpm发起请求执行任意文件。

php-fpm可以理解为fastcgi协议解析器，Nginx等服务器中间件将用户请求按照fastcgi的规则打包好通过TCP传给php-fpm，php-fpm按照fastcgi协议将TCP流解析成真正的数据。

例如：如果我们请求http://127.0.0.1/index.php?a=1&b=2，如果web目录是/var/www/html，那么Nginx会根据该请求生成如下key-value对：

`
{
    'GATEWAY_INTERFACE': 'FastCGI/1.0',
    'REQUEST_METHOD': 'GET',
    'SCRIPT_FILENAME': '/var/www/html/index.php',
    'SCRIPT_NAME': '/index.php',
    'QUERY_STRING': '?a=1&b=2',
    'REQUEST_URI': '/index.php?a=1&b=2',
    'DOCUMENT_ROOT': '/var/www/html',
    'SERVER_SOFTWARE': 'php/fcgiclient',
    'REMOTE_ADDR': '127.0.0.1',
    'REMOTE_PORT': '12345',
    'SERVER_ADDR': '127.0.0.1',
    'SERVER_PORT': '80',
    'SERVER_NAME': "localhost",
    'SERVER_PROTOCOL': 'HTTP/1.1'
}
`

SCRIPT_FILENAME的值指向的PHP文件将会被执行，也就是/var/www/html/index.php。

因此，如果php-fpm对外暴露时，我们通过构造fastcgi协议直接发送给pfp-fpm，将会执行"任意文件"。
值得注意的是，这个任意文件必须是目标服务器上的文件。
如果想执行任意文件的话需要借助auto_prepend_file和auto_append_file。

auto_prepend_file是告诉PHP，在执行目标文件之前，先包含auto_prepend_file中指定的文件；
auto_append_file是告诉PHP，在执行完成目标文件后，包含auto_append_file指向的文件。

假设我们设置auto_prepend_file为php://input，那么就等于在执行任何php文件前都要包含一遍POST的内容。
所以，只需要把待执行的代码放在Body中就能被执行了。（当然，还需要开启远程文件包含选项allow_url_include）

那么如何修改这两个值呢？

这又涉及到PHP-FPM的两个环境变量，PHP_VALUE和PHP_ADMIN_VALUE。
这两个环境变量就是用来设置PHP配置项的，PHP_VALUE可以设置模式为PHP_INI_USER和PHP_INI_ALL的选项，PHP_ADMIN_VALUE可以设置所有选项。
经过测试，disable_functions除外，这个选项是PHP加载的时候就确定了，在范围内的函数直接不会被加载到PHP上下文中，貌似无法修改。

```
{
    'GATEWAY_INTERFACE': 'FastCGI/1.0',
    'REQUEST_METHOD': 'GET',
    'SCRIPT_FILENAME': '/var/www/html/index.php',
    'SCRIPT_NAME': '/index.php',
    'QUERY_STRING': '?a=1&b=2',
    'REQUEST_URI': '/index.php',
    'DOCUMENT_ROOT': '/var/www/html',
    'SERVER_SOFTWARE': 'php/fcgiclient',
    'REMOTE_ADDR': '127.0.0.1',
    'REMOTE_PORT': '12345',
    'SERVER_ADDR': '127.0.0.1',
    'SERVER_PORT': '80',
    'SERVER_NAME': "localhost",
    'SERVER_PROTOCOL': 'HTTP/1.1'
    'PHP_VALUE': 'auto_prepend_file = php://input',
    'PHP_ADMIN_VALUE': 'allow_url_include = On'
}
```

## 二、攻击demo

见文件夹exp。

## 三、检测方式

1. 检查PHP_VALUE和PHP_ADMIN_VALUE中以上敏感字段是否被修改过。
2. 由于可能存在多个worker，因此通过循环请求多次。