function convertToRegistryRequest(csvRow) {
  return {
    partnerId: csvRow.partnerId,
    locationId: csvRow.locationId,
    description: csvRow.description,
    phoneNumbers: csvRow.phoneNumbers ? csvRow.phoneNumbers.split('|') : [],
    email: csvRow.email || null,
    openingTime: csvRow.openingTime,
    startValidity: csvRow.startValidity,
    endValidity: csvRow.endValidity,
    externalCodes: csvRow.externalCodes ? csvRow.externalCodes.split('|') : [],
    appointmentRequired: csvRow.appointmentRequired === 'true',
    website: csvRow.website || null,
    partnerType: csvRow.partnerType,
    address: {
      addressRow: csvRow.addressRow?.replace(/^"|"$/g, ''),
      cap: csvRow.cap,
      city: csvRow.city,
      province: csvRow.province,
      country: csvRow.country
    }
  };
}

function findLocationId(apiRegistries, csvRegistry) {
  return apiRegistries.find(r => r.locationId === csvRegistry.locationId)?.locationId;
}

module.exports = { convertToRegistryRequest, findLocationId };
