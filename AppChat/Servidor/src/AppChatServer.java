import java.io.*;
import java.net.*;
import java.util.*;

public class AppChatServer {
    // Define a porta do servidor
    private static final int PORT = 12345;
    // Cria um servidor de socket para escutar as conexões
    private static ServerSocket serverSocket;
    // Armazena os clientes conectados (nome do cliente e o PrintWriter para enviar
    // mensagens)
    private static Map<String, PrintWriter> clients = new HashMap<>();
    // Armazena os nomes dos clientes conectados
    private static Set<String> clientNames = new HashSet<>();

    public static void main(String[] args) throws Exception {
        try {
            // Inicia o ServerSocket para escutar na porta definida
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor aguardando conexões...");

            // Fica em loop esperando por conexões de clientes
            while (true) {
                // Aceita a conexão de um cliente
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());

                // Cria uma nova thread para gerenciar esse cliente
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Classe interna que gerencia a comunicação com cada cliente
    private static class ClientHandler implements Runnable {
        private Socket socket; // Socket do cliente
        private BufferedReader in; // Lê as mensagens do cliente
        private PrintWriter out; // Envia mensagens para o cliente
        private String clientName; // Nome do cliente

        // Construtor da classe, recebe o socket do cliente
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                // Cria os streams de entrada e saída para comunicação com o cliente
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Método que fica aguardando e processando as mensagens do cliente
        public void run() {
            try {
                // Envia um pedido para o cliente informar seu nome
                out.println("Digite seu nome:");
                // Lê o nome do cliente
                clientName = in.readLine();
                // Sincroniza o acesso à lista de clientes para evitar problemas em um ambiente
                // multithread
                synchronized (clients) {
                    // Adiciona o cliente e sua saída ao mapa de clientes
                    clients.put(clientName, out);
                    // Adiciona o nome do cliente ao conjunto de nomes
                    clientNames.add(clientName);
                }

                // Atualiza a lista de clientes para todos os clientes conectados
                updateClientList();

                System.out.println(clientName + " entrou no chat.");

                // Fica aguardando mensagens do cliente
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            String target = parts[1]; // Destinatário
                            String msg = parts[2]; // Mensagem
                            sendMessageToClient(target, msg); // Envia a mensagem para o destinatário
                        }
                    } else if (message.startsWith("/todos")) {
                        // Se o comando for "/todos", envia para todos os clientes
                        String msg = message.substring(7); // Extrai a mensagem após "/todos"
                        sendMessageTodosClients(clientName + " (para todos): " + msg);
                    } else if (message.startsWith("/list")) {
                        // Comando "/list" para listar todos os clientes
                        sendClientList();
                    } else {
                        // Mensagens normais (sem comando)
                        System.out.println(clientName + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Remover cliente da lista ao desconectar
                synchronized (clients) {
                    // Remove o cliente da lista de clientes conectados
                    clients.remove(clientName);
                    // Remove o nome do cliente da lista de nomes
                    clientNames.remove(clientName);
                }
                // Notifica todos os clientes sobre a saída do cliente
                notifyClientsOfExit();
                try {
                    // Fecha o socket do cliente
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Método que envia uma mensagem para todos os clientes conectados
        private void sendMessageTodosClients(String message) {
            // Usando o bloco synchronized para garantir que a operação seja segura em um
            // ambiente multi-thread
            synchronized (clients) {
                // Itera sobre todos os PrintWriter dos clientes conectados (armazena as saídas
                // de cada cliente)
                for (PrintWriter writer : clients.values()) {
                    // Envia a mensagem para cada cliente através do PrintWriter
                    writer.println(message);
                }
            }
        }

        // Método que envia a lista de clientes conectados para o cliente que solicitou
        private void sendClientList() {
            // StringBuilder é usado para construir a mensagem de forma eficiente
            StringBuilder clientListMessage = new StringBuilder("Clientes conectados: ");

            // Usando o bloco synchronized para garantir que a operação seja segura em um
            // ambiente multi-thread
            synchronized (clientNames) {
                // Itera sobre todos os nomes dos clientes conectados (armazenados em
                // clientNames)
                for (String name : clientNames) {
                    // Adiciona o nome de cada cliente à lista de clientes conectados, seguido de
                    // uma vírgula
                    clientListMessage.append(name).append(", ");
                }
            }

            // Remove a última vírgula e o espaço adicionados no final da lista de clientes
            if (clientListMessage.length() > 0) {
                clientListMessage.setLength(clientListMessage.length() - 2); // Ajusta o comprimento da StringBuilder
                                                                             // para remover a última vírgula e o espaço
            }

            // Envia a lista de clientes conectados de volta ao cliente que solicitou
            out.println(clientListMessage.toString());
        }

        // Envia uma mensagem para um cliente específico
        private void sendMessageToClient(String target, String message) {
            // Obtém o PrintWriter do destinatário
            PrintWriter targetOut = clients.get(target);
            // Se o destinatário estiver conectado, envia a mensagem
            if (targetOut != null) {
                targetOut.println(clientName + " diz: " + message);
            } else {
                // Se o destinatário não estiver encontrado, avisa o cliente
                out.println("Usuário " + target + " não encontrado");
            }
        }

        // Atualiza a lista de clientes conectados para todos os clientes
        private void updateClientList() {
            StringBuilder clientListMessage = new StringBuilder(clientName + " entrou no chat...");

            // Envia a mensagem de cliente entrando para todos os clientes conectados
            synchronized (clients) {
                for (PrintWriter writer : clients.values()) {
                    writer.println(clientListMessage.toString());
                }
            }
        }

        // Notifica todos os clientes sobre a desconexão de um cliente
        private void notifyClientsOfExit() {
            String exitMessage = clientName + " saiu do chat.";

            // Envia a mensagem de saída para todos os clientes conectados
            synchronized (clients) {
                for (PrintWriter writer : clients.values()) {
                    writer.println(exitMessage);
                }
            }
        }
    }
}
