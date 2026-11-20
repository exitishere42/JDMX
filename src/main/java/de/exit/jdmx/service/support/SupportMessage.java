package de.exit.jdmx.service.support;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Repräsentiert eine Support-, Feedback- oder Bug-Nachricht in JDMX.
 */
public final class SupportMessage {

    /**
     * Übertragungsstatus einer Support-Nachricht.
     */
    public enum MessageStatus {
        /** Ausstehend / Entwurf. */
        PENDING,
        /** Wird aktuell übermittelt. */
        SENDING,
        /** Erfolgreich an Webhook übermittelt. */
        SENT,
        /** Übertragung fehlgeschlagen. */
        FAILED
    }

    private final String id;
    private final SupportCategory category;
    private final String senderName;
    private final String senderContact;
    private final String messageText;
    private final Instant timestamp;
    private final SupportSystemInfo systemInfo;
    private volatile MessageStatus status;

    /**
     * Erstellt eine neue SupportMessage mit allen Parametern.
     *
     * @param id            Eindeutige ID der Nachricht
     * @param category      Kategorie (Feedback oder Bug)
     * @param senderName    Name des Absenders (oder Fallback "Anonym")
     * @param senderContact Kontaktadresse (E-Mail, Discord etc. oder "-")
     * @param messageText   Eigentlicher Nachrichteninhalt
     * @param timestamp     Erstellungszeitpunkt
     * @param systemInfo    Optionale System- und Diagnoseinfos
     * @param status        Aktueller Übertragungsstatus
     */
    public SupportMessage(
            String id,
            SupportCategory category,
            String senderName,
            String senderContact,
            String messageText,
            Instant timestamp,
            SupportSystemInfo systemInfo,
            MessageStatus status
    ) {
        this.id = Objects.requireNonNull(id, "ID darf nicht null sein");
        this.category = Objects.requireNonNull(category, "Kategorie darf nicht null sein");
        this.senderName = (senderName == null || senderName.isBlank()) ? "Anonym" : senderName.trim();
        this.senderContact = (senderContact == null || senderContact.isBlank()) ? "-" : senderContact.trim();
        if (messageText == null || messageText.isBlank()) {
            throw new IllegalArgumentException("Nachricht darf nicht leer sein");
        }
        this.messageText = messageText.trim();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.systemInfo = systemInfo;
        this.status = status != null ? status : MessageStatus.PENDING;
    }

    /**
     * Bequeme Fabrikmethode zur Erstellung einer neuen Nachricht mit automatischer UUID und PENDING-Status.
     *
     * @param category      Kategorie
     * @param senderName    Name des Absenders
     * @param senderContact Kontaktadresse
     * @param messageText   Inhalt
     * @param systemInfo    Optionale Systemdaten
     * @return Neue {@link SupportMessage}
     */
    public static SupportMessage create(
            SupportCategory category,
            String senderName,
            String senderContact,
            String messageText,
            SupportSystemInfo systemInfo
    ) {
        return new SupportMessage(
                "msg-" + UUID.randomUUID().toString().substring(0, 8),
                category,
                senderName,
                senderContact,
                messageText,
                Instant.now(),
                systemInfo,
                MessageStatus.PENDING
        );
    }

    public String id() {
        return id;
    }

    public SupportCategory category() {
        return category;
    }

    public String senderName() {
        return senderName;
    }

    public String senderContact() {
        return senderContact;
    }

    public String messageText() {
        return messageText;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public SupportSystemInfo systemInfo() {
        return systemInfo;
    }

    public MessageStatus status() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupportMessage that = (SupportMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SupportMessage{" +
                "id='" + id + '\'' +
                ", category=" + category +
                ", senderName='" + senderName + '\'' +
                ", status=" + status +
                '}';
    }
}
