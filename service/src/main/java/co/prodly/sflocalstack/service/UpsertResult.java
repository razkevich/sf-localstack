package co.prodly.sflocalstack.service;

import co.prodly.sflocalstack.model.SObjectRecord;

public record UpsertResult(SObjectRecord record, boolean created) {
}
