package HandGame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static int port = 9004;
    private static String player1Name;
    private static String player2Name;
    private static Socket player1Socket;
    private static Socket player2Socket;
    private static PrintWriter out1;
    private static PrintWriter out2;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("서버 시작됨. 클라이언트 연결 대기 중...");

            player1Socket = serverSocket.accept();
            System.out.println("플레이어 1 연결됨");
            out1 = new PrintWriter(player1Socket.getOutputStream(), true);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(player1Socket.getInputStream()));
            out1.println("isPlayer1"); // 첫 번째 클라이언트가 Player1임을 알림

            player2Socket = serverSocket.accept();
            System.out.println("플레이어 2 연결됨");
            out2 = new PrintWriter(player2Socket.getOutputStream(), true);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(player2Socket.getInputStream()));

            // 플레이어 1 이름 수신
            player1Name = in1.readLine();

            // 플레이어 2 이름 수신
            player2Name = in2.readLine();

            // 서로의 이름을 각 플레이어에게 전송
            out1.println("PLAYER2:" + player2Name);
            out2.println("PLAYER2:" + player1Name);

            // 클라이언트의 메시지를 처리하는 스레드 시작
            new Thread(() -> handleClient(player1Socket, in1, out2)).start();
            new Thread(() -> handleClient(player2Socket, in2, out1)).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket, BufferedReader in, PrintWriter out) {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
