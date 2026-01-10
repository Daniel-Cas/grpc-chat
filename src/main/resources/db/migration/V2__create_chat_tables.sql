-- Create chat tables
CREATE TYPE CHAT_TYPE AS ENUM ('DIRECT', 'GROUP', 'ROOM');
CREATE TYPE VISIBILITY_TYPE AS ENUM ('PRIVATE', 'VISIBLE');
CREATE TYPE CHAT_ROLE AS ENUM ('ADMIN', 'OWNER', 'MEMBER');

CREATE TABLE chats (
    id BIGSERIAL PRIMARY KEY,
    type CHAT_TYPE NOT NULL,
    created_by BIGINT CONSTRAINT fk_chat_user_id REFERENCES users ON UPDATE RESTRICT ON DELETE RESTRICT,
    description VARCHAR(128),
    name VARCHAR(100) NOT NULL,
    visibility VISIBILITY_TYPE NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (name)
);

CREATE INDEX idx_chats_type ON chats(type);

CREATE TABLE chat_members (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT CONSTRAINT fk_chat_member_user_id REFERENCES users ON UPDATE RESTRICT ON DELETE RESTRICT,
    chat_id BIGINT CONSTRAINT fk_chat_id REFERENCES chats ON UPDATE RESTRICT ON DELETE RESTRICT,
    active BOOLEAN NOT NULL DEFAULT true,
    role CHAT_ROLE NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (chat_id, user_id)
);

CREATE INDEX idx_chat_members_user ON chat_members(user_id);
CREATE INDEX idx_chat_members_chat ON chat_members(chat_id);

CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT CONSTRAINT fk_chat_id REFERENCES chats ON UPDATE RESTRICT ON DELETE RESTRICT,
    sender_id BIGINT CONSTRAINT fk_user_id REFERENCES users ON UPDATE RESTRICT ON DELETE RESTRICT,
    reply_to_id BIGINT CONSTRAINT fk_message_id REFERENCES messages ON UPDATE RESTRICT ON DELETE RESTRICT,
    client_message_id VARCHAR(64),
    content TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_client_message UNIQUE (chat_id, sender_id, client_message_id)
);

CREATE INDEX idx_messages_chat_id_id ON messages(chat_id, id DESC);
CREATE INDEX idx_messages_chat_id_sent_at ON messages(chat_id, sent_at DESC);
CREATE INDEX idx_messages_sender_id ON messages(sender_id);