module.exports = {
  API_BASE_URL: process.env.API_BASE_URL,
  HEADERS: {
    'Content-Type': 'application/json',
    'x-pagopa-pn-cx-id': 'cx-id', // TODO da rimuovere
    'uid': 'uid' // TODO da rimuovere
  }
};