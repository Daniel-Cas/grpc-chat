## Entity relationship
```mermaid
erDiagram
user {
	bigserial id PK
	username varchar UK
	password varchar
	email varchar
	created_at timestamp
	updated_at timestamp
}

chat {
	bigserial id PK
	enum type
	created_at timestamp
}

chat_member {
	bigserial id PK
	bigint user_id FK
	bigint chat_id FK
}

message {
	bigserial id PK
	bigint chat_id FK
	bigint sender_id FK
	text content
	timestamp send_at
}

user ||--o{ chat_member : "is_member"
chat ||--o{ chat_member : "has_members"
chat ||--o{ message : "has_messages"
user ||--o{ message : "sends"
```

## Main sequence diagram high level
```mermaid
sequenceDiagram
    autonumber

    participant C as Cliente
    participant S as Servidor gRPC

    Note over C,S: ===== 1. Abrir conexión del stream =====
    C->>S: OpenStream()
    S-->>C: ConnectionAck

    Note over C,S: ===== 2. Suscripción a un chat =====
    C->>S: Subscription{chat_id, user_id}
    S->>S: Registrar suscriptor (in-memory)
    S-->>C: Subscription{ok=true, members=[UserProfile...]}

    Note over C,S: ===== 3. Cliente pide historial =====
    C->>S: MessageHistoryRequest{chat_id, limit, before_id?}
    S->>S: Query mensajes desde DB
    S-->>C: MessageHistory{messages=[...]}

    Note over C,S: ===== 4. Cliente envía mensaje =====
    C->>S: MessageSent{client_message_id, chat_id, content}
    S->>S: Persistir mensaje en DB
    S-->>C: MessageCreated{client_message_id, message}
    
    Note over C,S: ===== 5. Servidor hace broadcast =====
    S-->>C: MessageCreated{message} (si el cliente está suscrito)
    S-->>O: MessageCreated{message} (otros clientes suscritos)

    Note over C,S: ===== 6. Flujo continúa hasta cerrar stream =====
    C->>S: Stream close
    S->>S: Eliminar suscriptor del chat
```


## **Componentes:**
1. **Version** (`v4`): Versión del protocolo
2. **Purpose**: 
   - `local`: Cifrado simétrico (compartir clave secreta)
   - `public`: Firma asimétrica (clave privada/pública)
3. **Payload**: Datos cifrados/firmados (base64url)
4. **Footer** (opcional): Metadata pública no protegida

### Paseto v4: Dos Modos

#### 1. **v4.local** (Cifrado Simétrico) 🔐

**Cuándo usarlo:**
- Tokens para tu propio sistema
- Service-to-service en misma org
- Cuando emisor = validador

**Criptografía:**
- **Algoritmo**: XChaCha20-Poly1305 (AEAD)
- **Clave**: 256 bits (32 bytes) simétrica
- **Garantías**: 
  - Confidencialidad (nadie puede leer el payload)
  - Autenticidad (no se puede modificar)
  - Protección contra replay (con implicit assertion)

### **Flujo:**

#### Emisor (tiene clave K):
1. Payload = {"user": "john", "exp": "..."}
2. Token = encrypt(Payload, K, nonce)
3. Envía: v4.local.EncryptedPayload

#### Validador (tiene misma clave K):
1. Recibe: v4.local.EncryptedPayload
2. Payload = decrypt(Token, K)
3. Valida claims (exp, nbf, etc.)

#### 2. **v4.public** (Firma Asimétrica)

**Cuándo usarlo:**
- Tokens para terceros
- APIs públicas
- Cuando emisor ≠ validador
- Múltiples servicios validando

**Criptografía:**
- **Algoritmo**: Ed25519 (firma digital)
- **Claves**: Par privada (firma) / pública (verifica)
- **Garantías**:
  - Autenticidad (firmado por quien tiene clave privada)
  - Integridad (no modificado)
  - NO confidencialidad (payload es visible)

### **Flujo:**

#### Emisor (tiene clave privada):
1. Payload = {"user": "john", "exp": "..."}
2. Token = sign(Payload, PrivateKey)
3. Envía: v4.public.SignedPayload

#### Validador (tiene clave pública):
1. Recibe: v4.public.SignedPayload
2. verify(Token, PublicKey) → Payload o error
3. Valida claims