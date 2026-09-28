package com.example.data.model

enum class FixedPostType(val labelFr: String, val shortDesc: String) {
    ONE_TIME("Forfait unique fixe", "Montant fixe global"),
    PER_DAY("Par jour restant", "Montant × jours restants applicables"),
    PER_WORKER("Par travailleur", "Montant × effectif total"),
    PER_DAY_PER_WORKER("Par travailleur × par jour", "Montant × effectif × jours restants"),
    CUSTOM_AMOUNT("Montant personnalisé", "Montant saisi manuellement");
}
