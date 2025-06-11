import java.awt.*; // Importa os pacotes relacionados à interface gráfica
import java.awt.event.*; // Importa os pacotes para manipulação de eventos
import java.io.*; // Importa as classes para manipulação de I/O
import javax.swing.*; // Importa as classes para construir a interface gráfica com Swing
import javax.swing.table.DefaultTableModel; // Importa o modelo de tabela para Swing
import java.util.List; // Importa as classes para listas
import java.net.*; // Importa classes de rede (Sockets)
import java.text.SimpleDateFormat; // Importa para manipulação de datas e horários
import java.util.ArrayList; // Importa a classe ArrayList
import java.util.Date; // Importa a classe Date para obter data e hora atuais

public class AppChatCliente extends JFrame { // Classe principal que herda de JFrame para criar a interface
    private JTextArea taChat; // Área de texto onde as mensagens são exibidas
    private JTextField tfMessage; // Campo de texto onde o usuário digita a mensagem
    private JTextField tfRecipient; // Campo de texto onde o usuário digita o destinatário
    private JButton btnSend; // Botão de envio de mensagem
    private JTable clientTable; // Tabela que mostra os clientes conectados
    private DefaultTableModel tableModel; // Modelo para a tabela
    private Socket socket; // Objeto para representar a conexão com o servidor
    private PrintWriter out; // Objeto para enviar mensagens para o servidor
    private BufferedReader in; // Objeto para ler mensagens do servidor
    private String serverAddress = "10.74.241.195"; // Endereço IP do servidor (por padrão)
    private int port = 12345; // Porta do servidor
    private List<ClientInfo> clients; // Lista para armazenar os clientes conectados
    private String clientName; // Nome do cliente

    public AppChatCliente() { // Construtor da classe
        clients = new ArrayList<>(); // Inicializa a lista de clientes

        // Configurações da janela principal
        setTitle("Chat Client"); // Define o título da janela
        setSize(400, 300); // Define o tamanho da janela
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Define a operação de fechamento da janela
        setLayout(new BorderLayout()); // Define o layout da janela

        taChat = new JTextArea(); // Cria a área de texto para o chat
        taChat.setEditable(false); // Impede que o usuário edite o texto na área de chat
        add(new JScrollPane(taChat), BorderLayout.CENTER); // Adiciona a área de texto na parte central da janela

        JPanel panel = new JPanel(); // Cria um painel para organizar os elementos na parte inferior
        panel.setLayout(new BorderLayout()); // Define o layout do painel

        // Adicionando o label e o campo de texto para o destinatário
        JPanel recipientPanel = new JPanel(); // Painel para o campo do destinatário
        recipientPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Define o layout
        JLabel lblRecipient = new JLabel("Destinatário:"); // Label indicando "Destinatário"
        recipientPanel.add(lblRecipient); // Adiciona o label ao painel
        tfRecipient = new JTextField(19); // Campo de texto para o destinatário
        recipientPanel.add(tfRecipient); // Adiciona o campo de texto ao painel
        panel.add(recipientPanel, BorderLayout.NORTH); // Adiciona o painel na parte superior do painel principal

        // Adicionando o label e o campo de texto para a mensagem
        JPanel messagePanel = new JPanel(); // Painel para o campo de mensagem
        messagePanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Define o layout
        JLabel lblMessage = new JLabel("Mensagem:"); // Label indicando "Mensagem"
        messagePanel.add(lblMessage); // Adiciona o label ao painel
        tfMessage = new JTextField(20); // Campo de texto para a mensagem
        messagePanel.add(tfMessage); // Adiciona o campo de texto ao painel
        panel.add(messagePanel, BorderLayout.CENTER); // Adiciona o painel na parte central do painel principal

        // Botão de envio
        btnSend = new JButton("Enviar"); // Cria o botão de envio
        panel.add(btnSend, BorderLayout.EAST); // Adiciona o botão ao painel

        add(panel, BorderLayout.SOUTH); // Adiciona o painel à parte inferior da janela

        // Criação do modelo da tabela, tornando as células não editáveis
        tableModel = new DefaultTableModel(new Object[] { "Cliente", "Hora de Conexão" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { // Impede que as células da tabela sejam editadas
                return false;
            }
        };

        clientTable = new JTable(tableModel); // Cria a tabela de clientes conectados
        JScrollPane tableScrollPane = new JScrollPane(clientTable); // Adiciona a tabela em um JScrollPane para permitir
                                                                    // rolagem
        tableScrollPane.setPreferredSize(new Dimension(200, 100)); // Define o tamanho da área de rolagem
        add(tableScrollPane, BorderLayout.WEST); // Adiciona a tabela à parte esquerda da janela

        // Ação do botão de enviar
        btnSend.addActionListener(new ActionListener() { // Define a ação quando o botão de envio for clicado
            public void actionPerformed(ActionEvent e) {
                sendMessage(); // Chama o método para enviar a mensagem
            }
        });

        // Adicionando KeyListener ao campo de mensagem para enviar ao pressionar Enter
        tfMessage.addKeyListener(new KeyAdapter() { // Adiciona o listener para detectar quando a tecla Enter for
                                                    // pressionada
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { // Verifica se a tecla pressionada foi Enter
                    sendMessage(); // Chama o método para enviar a mensagem
                }
            }
        });

        // Conectar ao servidor
        connectToServer(); // Chama o método para conectar ao servidor

        // Iniciar a thread de recebimento de mensagens
        new Thread(new MessageReceiver()).start(); // Cria e inicia uma thread para receber mensagens do servidor

        // Listener para detectar quando o cliente fecha a interface
        addWindowListener(new WindowAdapter() { // Adiciona um listener para detectar o fechamento da janela
            public void windowClosing(WindowEvent e) {
                sendExitMessage(); // Envia uma mensagem informando que o cliente saiu
                closeConnection(); // Fecha a conexão com o servidor
            }
        });
    }

    // Método para conectar ao servidor
    private void connectToServer() {
        try {
            // Selecionar o Servidor
            String iphost = JOptionPane.showInputDialog("Digite o ip do servidor:"); // Solicita o IP do servidor
            serverAddress = iphost; // Define o IP do servidor
            socket = new Socket(serverAddress, port); // Cria o socket e conecta ao servidor
            out = new PrintWriter(socket.getOutputStream(), true); // Inicializa o objeto de saída
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Inicializa o objeto de entrada

            // Enviar o nome do cliente
            clientName = JOptionPane.showInputDialog("Digite seu nome:"); // Solicita o nome do cliente
            setTitle("Chat Client - " + clientName); // Altera o título da janela com o nome do cliente
            out.println(clientName); // Envia o nome do cliente para o servidor

            // Adicionar cliente à lista e atualizar a tabela
            ClientInfo newClient = new ClientInfo(clientName, getCurrentTime()); // Cria um novo cliente com o nome e
                                                                                 // hora
            clients.add(newClient); // Adiciona o cliente à lista
            updateClientTable(); // Atualiza a tabela com os clientes conectados
        } catch (IOException e) { // Trata exceções de I/O
            e.printStackTrace();
        }
    }

    // Método para enviar uma mensagem
    private void sendMessage() {
        String recipient = tfRecipient.getText(); // Obtém o destinatário da mensagem
        String message = tfMessage.getText(); // Obtém a mensagem digitada
        if (recipient.equals("/todos")) {
            // Se o destinatário for "/todos", envia a mensagem para todos os clientes
            out.println("/todos " + message);
            taChat.append("Você (para todos): " + message + "\n");
        } else if (recipient.equals("/list")) {
            // Se o destinatário for "/list", envia o comando para o servidor listar os
            // clientes
            out.println("/list");
            taChat.append("Você: Listando todos os clientes conectados...\n");
        } else if (!message.isEmpty() && !recipient.isEmpty()) {
            // Comando normal para enviar mensagem para o destinatário
            out.println("/send " + recipient + " " + message);
            taChat.append("Você (para " + recipient + "): " + message + "\n");
        } else if (!message.isEmpty()) {
            // Caso seja uma mensagem geral (sem destinatário)
            out.println(message);
            taChat.append("Você: " + message + "\n");
        }

        tfMessage.setText(""); // Limpa o campo de mensagem
    }

    // Método para enviar uma mensagem de saída
    private void sendExitMessage() {
        out.println(clientName + " saiu do chat..."); // Envia uma mensagem informando que o cliente saiu
    }

    // Método para fechar a conexão com o servidor
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close(); // Fecha a conexão
            }
        } catch (Exception e) { // Trata exceções ao tentar fechar a conexão
            e.printStackTrace();
        }
    }

    // Método para atualizar a tabela com a lista de clientes
    private void updateClientTable() {
        tableModel.setRowCount(0); // Limpa a tabela existente

        // Adiciona todos os clientes à tabela
        for (ClientInfo client : clients) {
            tableModel.addRow(new Object[] { client.getName(), client.getConnectionTime() }); // Adiciona o nome e hora
                                                                                              // de conexão de cada
                                                                                              // cliente
        }
    }

    // Método para obter a hora atual
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // Define o formato da hora
        return sdf.format(new Date()); // Retorna a hora atual formatada
    }

    // Classe para receber mensagens do servidor
    private class MessageReceiver implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) { // Lê as mensagens do servidor
                    taChat.append(message + "\n"); // Exibe as mensagens recebidas na área de chat
                }
            } catch (Exception e) { // Trata exceções ao ler as mensagens
                e.printStackTrace();
            }
        }
    }

    // Classe para armazenar as informações do cliente
    class ClientInfo {
        private String name; // Nome do cliente
        private String connectionTime; // Hora de conexão do cliente

        public ClientInfo(String name, String connectionTime) {
            this.name = name;
            this.connectionTime = connectionTime;
        }

        public String getName() {
            return name; // Retorna o nome do cliente
        }

        public String getConnectionTime() {
            return connectionTime; // Retorna a hora de conexão do cliente
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() { // Cria e exibe a janela principal
            public void run() {
                AppChatCliente app = new AppChatCliente(); // Cria a instância da aplicação
                app.setVisible(true); // Exibe a janela
                app.setLocationRelativeTo(null); // Centraliza a janela na tela
            }
        });
    }
}
