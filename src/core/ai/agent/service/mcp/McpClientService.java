package ai.agent.service.mcp;

import ai.agent.dto.mcp.McpResponse;
import cn.hutool.json.JSONUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * MCP (Model Context Protocol) 客户端服务
 * 提供标准化的 WebSocket 通信接口，用于与 MCP 服务器交互
 */
public class McpClientService {

    // 子协议
    public static final String SUB_PROTOCOL = null;
//    public static final String SUB_PROTOCOL = "client_246";

    private final String wsUrl;
    private final int timeout;
    private final boolean debug;

    /**
     * 构造函数
     * @param wsUrl WebSocket 服务器地址 (例如: ws://183.6.70.7:8080)
     */
    public McpClientService(String wsUrl) {
        this(wsUrl, 60, false);
    }

    /**
     * 构造函数
     * @param wsUrl WebSocket 服务器地址
     * @param timeout 超时时间(秒)
     */
    public McpClientService(String wsUrl, int timeout) {
        this(wsUrl, timeout, false);
    }

    /**
     * 构造函数
     * @param wsUrl WebSocket 服务器地址
     * @param timeout 超时时间(秒)
     * @param debug 是否开启调试模式
     */
    public McpClientService(String wsUrl, int timeout, boolean debug) {
        this.wsUrl = wsUrl;
        this.timeout = timeout;
        this.debug = debug;
    }

    /**
     * 获取工具列表
     * @return MCP响应对象
     */
    public McpResponse listTools() throws Exception {
        String requestJson = buildJsonRpcRequest(2, "tools/list", new HashMap<>());
        String responseJson = sendRequest(requestJson);
        return parseResponse(responseJson);
    }

    /**
     * 获取工具列表 (返回原始JSON字符串)
     * @return 工具列表的 JSON 响应字符串
     */
    public String listToolsRaw() throws Exception {
        String requestJson = buildJsonRpcRequest(2, "tools/list", new HashMap<>());
        return sendRequest(requestJson);
    }

    /**
     * 调用 Codex 工具
     * @param prompt 提示词
     * @param workingDir 工作目录
     * @return MCP响应对象
     */
    public McpResponse callCodex(String prompt, String workingDir) throws Exception {
        return callCodex(prompt, workingDir, null);
    }

    /**
     * 调用 Codex 工具 (完整参数)
     * @param prompt 提示词
     * @param workingDir 工作目录
     * @param sessionId 会话ID (可选，用于继续之前的对话)
     * @return MCP响应对象
     */
    public McpResponse callCodex(String prompt, String workingDir, String sessionId) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("PROMPT", prompt);
        arguments.put("cd", workingDir);
        arguments.put("sandbox", "read-only");
        arguments.put("skip_git_repo_check", true);
        arguments.put("return_all_messages", false);
        arguments.put("SESSION_ID", sessionId);
        arguments.put("model", "gpt-5.1-codex-max");

        return callTool("codex", arguments);
    }


    /**
     * 调用指定的工具
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return MCP响应对象
     */
    public McpResponse callTool(String toolName, Map<String, Object> arguments) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        String requestJson = buildJsonRpcRequest(2, "tools/call", params);
        String responseJson = sendRequest(requestJson);
        return parseResponse(responseJson);
    }

    /**
     * 构建 JSON-RPC 请求
     */
    private String buildJsonRpcRequest(int id, String method, Map<String, Object> params) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"jsonrpc\": \"2.0\",");
        json.append("\"id\": ").append(id).append(",");
        json.append("\"method\": \"").append(method).append("\",");
        json.append("\"params\": ");
        json.append(mapToJson(params));
        json.append("}");
        return json.toString();
    }

    /**
     * 简单的 Map 转 JSON (仅支持基础类型)
     */
    private String mapToJson(Map<String, Object> map) {
        if (map.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();

            if (value == null) {
                json.append("null");
            } else if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(mapToJson((Map<String, Object>) value));
            } else {
                json.append("\"").append(value.toString()).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 解析JSON响应为DTO对象
     */
    private McpResponse parseResponse(String jsonResponse) {
        try {
            return JSONUtil.toBean(jsonResponse, McpResponse.class);
        } catch (Exception e) {
            // 解析失败，创建一个错误响应
            McpResponse errorResponse = new McpResponse();
            errorResponse.setJsonrpc("2.0");
            ai.agent.dto.mcp.McpError error = new ai.agent.dto.mcp.McpError();
            error.setCode(-1);
            error.setMessage("Failed to parse response: " + e.getMessage());
            error.setData(jsonResponse);
            errorResponse.setError(error);
            return errorResponse;
        }
    }

    /**
     * 发送请求到 MCP 服务器
     */
    private String sendRequest(String userTaskJson) throws Exception {
        URI uri = new URI(wsUrl);
        EventLoopGroup group = new NioEventLoopGroup();
        CompletableFuture<String> resultFuture = new CompletableFuture<>();

        try {
            // 设置最大帧大小为10MB，支持大响应
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, SUB_PROTOCOL, false,
                    new DefaultHttpHeaders(),
                    10 * 1024 * 1024); // maxFramePayloadLength = 10MB

            McpClientHandler handler = new McpClientHandler(handshaker, userTaskJson, resultFuture, debug);

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpClientCodec(),
                                    new HttpObjectAggregator(10 * 1024 * 1024), // 10MB，支持大响应
                                    handler);
                        }
                    });

            Channel ch = b.connect(uri.getHost(), uri.getPort()).sync().channel();
            handler.handshakeFuture().sync();

            return resultFuture.get(timeout, TimeUnit.SECONDS);

        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * MCP 协议处理器
     * 自动处理 WebSocket 握手、初始化和消息收发
     */
    static class McpClientHandler extends SimpleChannelInboundHandler<Object> {
        private final WebSocketClientHandshaker handshaker;
        private final String userTaskJson;
        private final CompletableFuture<String> resultFuture;
        private final boolean debug;
        private ChannelPromise handshakeFuture;
        private final StringBuilder responseBuilder = new StringBuilder();
        private boolean receivingResponse = false;

        public McpClientHandler(WebSocketClientHandshaker handshaker, String userTaskJson, CompletableFuture<String> resultFuture, boolean debug) {
            this.handshaker = handshaker;
            this.userTaskJson = userTaskJson;
            this.resultFuture = resultFuture;
            this.debug = debug;
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            Channel ch = ctx.channel();

            if (!handshaker.isHandshakeComplete()) {
                try {
                    handshaker.finishHandshake(ch, (io.netty.handler.codec.http.FullHttpResponse) msg);
                    handshakeFuture.setSuccess();
                    if (debug) {
                        System.out.println("=> [0] 连接建立，发送 Initialize...");
                    }
                    sendInitialize(ctx);
                } catch (WebSocketHandshakeException e) {
                    handshakeFuture.setFailure(e);
                    resultFuture.completeExceptionally(e);
                }
                return;
            }

            if (msg instanceof TextWebSocketFrame) {
                String response = ((TextWebSocketFrame) msg).text();

                if (debug) {
                    System.out.println("\n📨 收到服务器响应 (长度: " + response.length() + "): \n" + 
                        (response.length() > 500 ? response.substring(0, 500) + "..." : response));
                }

                if (response.contains("\"id\":1") || response.contains("\"id\": 1")) {
                    if (debug) {
                        System.out.println("=> [1] Init 成功，发送 notification 和 业务请求...");
                    }

                    String notify = "{\"jsonrpc\": \"2.0\", \"method\": \"notifications/initialized\"}";
                    ctx.writeAndFlush(new TextWebSocketFrame(notify + "\n"));
                    if (debug) {
                        System.out.println("📤 已发送 initialized 通知");
                    }

                    ctx.writeAndFlush(new TextWebSocketFrame(userTaskJson + "\n"));
                    if (debug) {
                        System.out.println("📤 已发送业务请求");
                    }
                    receivingResponse = true;
                }

                else if (response.contains("\"id\":2") || response.contains("\"id\": 2")) {
                    if (debug) {
                        System.out.println("✅ 收到业务请求响应");
                    }
                    resultFuture.complete(response);
                    ctx.close();
                }

                else if (response.contains("\"error\"")) {
                    System.err.println("❌ 服务端报错: " + response);
                    resultFuture.completeExceptionally(new RuntimeException("Server Error: " + response));
                    ctx.close();
                }
                else {
                    if (debug) {
                        System.out.println("ℹ️  收到其他消息(可能是通知): " + response);
                    }
                }
            } else if (msg instanceof CloseWebSocketFrame) {
                if (debug) {
                    System.out.println("🔒 服务器关闭连接");
                }
                if (receivingResponse && !resultFuture.isDone()) {
                    // 如果正在接收响应但连接被关闭，可能是服务端已完成
                    resultFuture.completeExceptionally(new RuntimeException("连接被服务器关闭，可能未收到完整响应"));
                }
                ctx.close();
            }
        }

        private void sendInitialize(ChannelHandlerContext ctx) {
            String initJson = "{" +
                    "\"jsonrpc\": \"2.0\"," +
                    "\"id\": 1," +
                    "\"method\": \"initialize\"," +
                    "\"params\": {" +
                    "\"protocolVersion\": \"2024-11-05\"," +
                    "\"capabilities\": {}," +
                    "\"clientInfo\": { \"name\": \"java-mcp-client\", \"version\": \"1.0\" }" +
                    "}" +
                    "}";

            ctx.writeAndFlush(new TextWebSocketFrame(initJson + "\n"));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (debug) {
                System.out.println("🔌 连接已断开");
            }
            if (!resultFuture.isDone()) {
                resultFuture.completeExceptionally(new RuntimeException("连接意外断开"));
            }
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            resultFuture.completeExceptionally(cause);
            ctx.close();
        }
    }
}
