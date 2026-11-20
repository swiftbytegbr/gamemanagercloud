import { Client } from "@stomp/stompjs";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const STOMP_URL = `wss://${API_BASE_URL}/websocket-frontend`;

export const stompClient = new Client({
    reconnectDelay: 5000,
    brokerURL: STOMP_URL,
    onStompError: (frame) => {
        console.error("Broker reported error: " + frame.headers["message"]);
        console.error("Additional details: " + frame.body);
    },
});
