package org.jabref.logic.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.os.OS;
import org.jabref.logic.remote.client.RemoteClient;
import org.jabref.logic.remote.server.RemoteListenerServerManager;
import org.jabref.logic.remote.server.RemoteMessageHandler;
import org.jabref.support.DisabledOnCIServer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisabledOnCIServer("Tests fails sporadically on CI server")
class RemoteSetupTest {

    private RemoteMessageHandler messageHandler;

    @BeforeEach
    void setUp() {
        messageHandler = mock(RemoteMessageHandler.class);
    }

    @Test
    void goodCase() {
        final int port = 34567;
        final String[] message = new String[]{"MYMESSAGE"};

        try (RemoteListenerServerManager server = new RemoteListenerServerManager()) {
            assertFalse(server.isOpen());
            server.openAndStart(messageHandler, port);
            assertTrue(server.isOpen());
            assertTrue(new RemoteClient(port).sendCommandLineArguments(message));
            verify(messageHandler).handleCommandLineArguments(message);
            server.stop();
            assertFalse(server.isOpen());
        }
    }

    @Test
    void goodCaseWithAllLifecycleMethods() {
        final int port = 34567;
        final String[] message = new String[]{"MYMESSAGE"};

        try (RemoteListenerServerManager server = new RemoteListenerServerManager()) {
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.open(messageHandler, port);
            assertTrue(server.isOpen());
            assertTrue(server.isNotStartedBefore());
            server.start();
            assertTrue(server.isOpen());
            assertFalse(server.isNotStartedBefore());

            assertTrue(new RemoteClient(port).sendCommandLineArguments(message));
            verify(messageHandler).handleCommandLineArguments(message);
            server.stop();
            assertFalse(server.isOpen());
            assertTrue(server.isNotStartedBefore());
        }
    }

    @Test
    void portAlreadyInUse() throws IOException {
        assumeFalse(OS.OS_X);

        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            assertTrue(socket.isBound());

            try (RemoteListenerServerManager server = new RemoteListenerServerManager()) {
                assertFalse(server.isOpen());
                server.openAndStart(messageHandler, port);
                assertFalse(server.isOpen());
                verify(messageHandler, never()).handleCommandLineArguments(any());
            }
        }
    }

    @Test
    void clientTimeout() {
        final int port = 34567;
        final String message = "MYMESSAGE";

        assertFalse(new RemoteClient(port).sendCommandLineArguments(new String[]{message}));
    }

    @Test
    void pingReturnsFalseForWrongServerListening() throws IOException, InterruptedException {
        final int port = 34567;

        try (ServerSocket socket = new ServerSocket(port)) {
            // Setup dummy server always answering "whatever"
            new Thread(() -> {
                try (Socket message = socket.accept(); OutputStream os = message.getOutputStream()) {
                    os.write("whatever".getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // Ignored
                }
            }).start();
            Thread.sleep(100);

            assertFalse(new RemoteClient(port).ping());
        }
    }

    @Test
    void pingReturnsFalseForNoServerListening() throws IOException, InterruptedException {
        final int port = 34567;

        assertFalse(new RemoteClient(port).ping());
    }

    @Test
    void pingReturnsTrueWhenServerIsRunning() {
        final int port = 34567;

        try (RemoteListenerServerManager server = new RemoteListenerServerManager()) {
            server.openAndStart(messageHandler, port);

            assertTrue(new RemoteClient(port).ping());
        }
    }
}
