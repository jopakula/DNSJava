package com.example.dnsjava;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {

    private static final String Tag = "JavaLog";

    private EditText editTextDomain;
    private EditText editTextDnsServer;
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextDomain = findViewById(R.id.editTextDomain);
        editTextDnsServer = findViewById(R.id.editTextServer);
        textViewResult = findViewById(R.id.textViewResult);

        Button buttonLookup = findViewById(R.id.buttonLookup);
        buttonLookup.setOnClickListener(view -> {
            Log.wtf(Tag, "button");
            String domain = editTextDomain.getText().toString();
            String dnsServer = editTextDnsServer.getText().toString();
            if (dnsServer.trim().isEmpty()) {
                editTextDnsServer.setText("8.8.8.8");
            }
            performDnsLookup(domain, dnsServer);
        });
    }

    private void performDnsLookup(String domain, String dnsServer) {
        new Thread(() -> {
            try {
                List<String> ips = new ArrayList<>();
                byte[] query = buildDnsQuery(domain);
                byte[] response = sendDnsQuery(query, dnsServer, 53);
                parseDnsResponse(response, ips);

                runOnUiThread(() -> {
                    textViewResult.setText(String.join("\n", ips));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    textViewResult.setText("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    private byte[] buildDnsQuery(String domain) {
        byte[] header = new byte[]{
                0, 0,
                1, 0,
                0, 1,
                0, 0,
                0, 0,
                0, 0,
        };

        String[] domainParts = domain.split("\\.");
        byte[] question = new byte[domain.length() + domainParts.length + 6];
        int index = 0;
        for (String part : domainParts) {
            question[index++] = (byte) part.length();
            System.arraycopy(part.getBytes(), 0, question, index, part.length());
            index += part.length();
        }
        question[index++] = 0;
        question[index++] = 0;
        question[index++] = 1;
        question[index++] = 0;
        question[index++] = 1;

        byte[] query = new byte[header.length + question.length];
        System.arraycopy(header, 0, query, 0, header.length);
        System.arraycopy(question, 0, query, header.length, question.length);

        return query;
    }

    private byte[] sendDnsQuery(byte[] query, String dnsServerIp, int dnsServerPort) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress dnsServerAddress = InetAddress.getByName(dnsServerIp);
        DatagramPacket packet = new DatagramPacket(query, query.length, dnsServerAddress, dnsServerPort);
        byte[] response = new byte[1024];

        DatagramPacket responsePacket = new DatagramPacket(response, response.length);
        socket.send(packet);
        socket.receive(responsePacket);
        socket.close();

        return response;
    }

    private void parseDnsResponse(byte[] response, List<String> ips) {
        int index = 12;
        while (response[index] != 0) index++;
        index += 5;
        int answersCount = response[7];
        for (int i = 0; i < answersCount; i++) {
            while (response[index] != 0) index++;
            index += 10;
            StringBuilder ip = new StringBuilder();
            for (int j = 0; j < 4; j++) {
                int octet = response[++index] & 0xFF;
                ip.append(octet);
                if (j < 3) {
                    ip.append(".");
                }
            }
            ips.add(ip.toString());
            index++;

            Log.wtf(Tag, "ip: " + ip);
            ip.setLength(0);
        }
    }
}
