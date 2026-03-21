package co.razkevich.sflocalstack.service;

import co.razkevich.sflocalstack.model.SObjectRecord;

public record UpsertResult(SObjectRecord record, boolean created) {
}
