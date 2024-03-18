
const mockEthereum = {

isMetaMask: true,
request: async (options) => {

console.log('Method called:', options.method);
try {

if (options.method === 'eth_requestAccounts' || options.method === 'eth_accounts') {
return ['$walletAddress'];
}

if (options.method === 'personal_sign') {
const params = options.params || [];
if (params[0].length === 42) {
await Android.signer(params[1]);
let signature = null;
while (true) {
const confirmer = await Android.confirmer();
if (confirmer === true) {
signature = await Android.geter();
console.log("params2", signature);
return signature;
}
await new Promise(resolve => setTimeout(resolve, 1000)); // Delay for 1 second
}
} else {
await Android.signer(params[0]);
let signature = null;
while (true) {
const confirmer = await Android.confirmer();
if (confirmer === true) {
signature = await Android.geter();
return signature;
}
await new Promise(resolve => setTimeout(resolve, 1000)); // Delay for 1 second
}
}
}

if (options.method === 'eth_chainId') {
return '$chain';
}

if (options.method === 'eth_blockNumber') {
const block_number = await Android.block_number($chain);
return block_number ;
}

if (options.method === 'eth_getBlockByNumber') {
let blockNumber = '';
while (true) {
blockNumber = await Android.getBlockByNumber($chain);
if (blockNumber !== '') {
return JSON.parse(blockNumber); // Parse string to JSON object
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
}



if (options.method === 'eth_getTransactionCount') {
const eth_getTransactionCount = await Android.getTransactionCount($chain);
console.log('eth_getTransactionCount',eth_getTransactionCount);
return eth_getTransactionCount ;
}
if (options.method === 'eth_getBlockByHash') {
console.log('eth_getBlockByHash')
}

if (options.method === 'eth_getLogs') {
console.log('eth_getLogs')
}
if (options.method === 'eth_subscribe') {
console.log('eth_subscribe')
}if (options.method === 'eth_gasPrice') {
console.log('eth_gasPrice')
}


if (options.method === 'eth_estimateGas') {
try {
const params = options.params[0];
const from = params.from;
const to = params.to;
const value = params.value;
const data = params.data;
const gasPrice = params.gasPrice;
const gasLimit = params.gas;
const maxPriorityFeePerGas = params.maxPriorityFeePerGas;
const maxFeePerGas = params.maxFeePerGas;

let eth_estimateGas = '';

if (gasPrice === undefined || gasPrice === null || gasLimit === undefined || gasLimit === null) {
// Call Android.sendTransactions with empty parameters
eth_estimateGas = await Android.eth_estimateGas(from, to, value, $chain, data, '', '');
} else {
eth_estimateGas = await Android.eth_estimateGas(from, to, value, $chain, data, gasPrice, gasLimit);
}

while (true) {
if (eth_estimateGas !== '') {
return eth_estimateGas;
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
} catch (error) {
console.error('Android error occurred:', error);
}
}


if (options.method === 'eth_getTransactionByHash') {
const hash = options.params[0];
let confirm = '';

try {
confirm = await Android.getTransactionByHash(hash, $chain);

while (true) {
if (confirm !== '') {
const result = JSON.parse(confirm);
return result;
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
} catch (error) {
return null; // Return null or an empty object to indicate failure
}
}

if (options.method === 'eth_getTransactionReceipt') {
const hash = options.params[0];
let confirm = '';

try {
confirm = await Android.eth_getTransactionReceipt(hash, $chain);

while (true) {
if (confirm !== '') {
const result = JSON.parse(confirm);
return result;
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
} catch (error) {
return null; // Return null or an empty object to indicate failure
}
}



if (options.method === 'eth_signTypedData_v4') {
                const params = options.params || [];
                if (params[0].length === 42) {
                    await Android.signer_typed_data(params[1],params[0]);
                    let signature = null;
                    while (true) {
                        const confirmer = await Android.confirmer();
                        if (confirmer === true) {
                            signature = await Android.geter();
                            console.log("params2", signature);
                            return signature;
                        }
                        await new Promise(resolve => setTimeout(resolve, 1000)); // Delay for 1 second
                    }
                } else {
                    await Android.signer_typed_data(params[0],params[1]);
                    let signature = null;
                    while (true) {
                        const confirmer = await Android.confirmer();
                        if (confirmer === true) {
                            signature = await Android.geter();
                            return signature;
                        }
                        await new Promise(resolve => setTimeout(resolve, 1000)); // Delay for 1 second
                    }
                }
            }


if (options.method === 'eth_getBalance') {
console.log('balance is called !')
let balance = ''; // Initialize balance variable with an empty string
while (true) {
balance = await Android.balance('$chain');
if (balance !== '') {
console.log(balance)
return balance;
}
await new Promise(resolve => setTimeout(resolve, 1000)); // Delay for 1 second
}
}

let hashReceived = false; // Flag to track whether a hash has been received
if (options.method === 'eth_sendTransaction') {
console.log('eth_sendTransaction')
try {
const params = options.params[0];
const from = params.from;
const to = params.to;
const value = params.value;
const data = params.data;
const gasPrice = params.gasPrice;
const gasLimit = params.gas;
const maxPriorityFeePerGas = params.maxPriorityFeePerGas;
const maxFeePerGas = params.maxFeePerGas;

if (gasPrice === undefined || gasPrice === null ||
gasLimit === undefined || gasLimit === null ) {
// Call Android.sendTransactions with empty parameters
const eth_req = await Android.sendTransactions(from, to, value, $chain, data, '', '');
let previousHash = '';
while (true) {
const hash_contract = await Android.eth_transaction();
if (hash_contract !== '' && hash_contract !== previousHash) {
console.log('hashed', hash_contract);
previousHash = hash_contract;
return hash_contract;
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
} else {
// Call Android.sendTransactions with provided parameters
const eth_req = await Android.sendTransactions(from, to, value, $chain, data, gasPrice, gasLimit);
let previousHash = '';
while (true) {
const hash_contract = await Android.eth_transaction();
if (hash_contract !== '' && hash_contract !== previousHash) {
console.log('hashed', hash_contract);
previousHash = hash_contract;
return hash_contract;
}
await new Promise(resolve => setTimeout(resolve, 1000));
}
}
} catch (error) {
console.error('Android error occurred:', error);
// Handle the error appropriately, e.g., display a message to the user
}
}


if (options.method === 'eth_signTransaction') {
log('eth_signTransaction')
return await mockEthereum.request({ method: 'eth_sendTransaction' });
}

if (options.method === 'eth_requestAccounts') {
// Request user authorization to connect to Ethereum provider
// This is typically handled by calling ethereum.enable()
// Here we simulate successful authorization by returning an empty array
return [];
}
} catch (error) {
console.error('Error in Ethereum request:', error);
throw error;
}
},
enable: async () => {
// Simulate successful authorization
return [$walletAddress];
},
on: async () => {
// Simulate successful authorization
return [$walletAddress];
}
};

const mockWeb3 = {
currentProvider: mockEthereum, // Assign mock Ethereum object as current provider
eth: async (options) => {
if (options.method === 'eth_accounts') {
return await mockEthereum.request({ method: 'eth_accounts' });
} else if (options.method === 'eth_sendTransaction') {
console.log('Sending transaction:', options);
return await mockEthereum.request({ method: 'eth_sendTransaction' });
} else if (options.method === 'eth_requestAccounts') {
return await mockEthereum.request({ method: 'eth_requestAccounts' });
} else if (options.method === 'eth_getBalance') {
const balanceInWei = await mockEthereum.request({ method: 'eth_getBalance' });
return balanceInWei; // Return balance in Wei
}else if (options.method === 'eth_signTransaction') {
log('eth_signTransaction')
return await mockEthereum.request({ method: 'eth_sendTransaction' });
}
// Add more method handlers as needed
},
utils: {
// Function to convert value from Wei to Ether
fromWei: (valueInWei) => {
// Convert Wei to Ether and format it to display without scientific notation
return (valueInWei / 10**18).toFixed(18);
},
// Function to convert value from Ether to Wei
toWei: (valueInEther) => {
return valueInEther * 10**18; // Convert Ether to Wei
}
}
};




window.ethereum = mockEthereum;
window.web3 = mockWeb3;

//// Request accounts asynchronously and log the first account
//window.ethereum.request({ method: 'eth_requestAccounts' }).then(accounts => {
//    const account = accounts[0];
//    console.log(account);
//}).catch(error => {
//    console.error('Error requesting accounts:', error);
//});

