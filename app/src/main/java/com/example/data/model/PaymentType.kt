package com.example.data.model

enum class PaymentType(val labelFr: String, val shortDesc: String) {
    DAILY("Paiement journalier", "Taux par jour × jours de travail"),
    HOURLY("Paiement horaire", "Taux par heure × heures prévues"),
    CUSTOM_AMOUNT("Montant personnalisé", "Forfait global ou montant spécifique");
}
