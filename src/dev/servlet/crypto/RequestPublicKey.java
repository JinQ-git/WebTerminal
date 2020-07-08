package dev.servlet.crypto;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import dev.util.crypto.RSAKeyManager;
import dev.util.crypto.RSAKeyManager.RSAKeyInfo;

@WebServlet("/crypto/pubkey.json")
public class RequestPublicKey extends HttpServlet
{
    private Gson gson = new Gson();
    private RSAKeyManager keyManager = RSAKeyManager.getInstance();

    private static final long serialVersionUID = 1L;
    public RequestPublicKey() { }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RSAKeyInfo info = keyManager.getDefaultKeyInfo();

        response.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.getWriter().write( gson.toJson(info) );
        
        // response.setContentType("application/x-pem-file");
        // response.getWriter().write(pubKeyPEM);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}