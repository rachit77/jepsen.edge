const Web3 = require("web3")
const fs = require('fs')

// npm run start -- sendtx https://rpc.ankr.com/eth_goerli 0x6acc43ba21cfff332106a9318e9ed08c11e7222273419c2c728dbe1d1a9aa032 0xcaeed488ad7e01286aa9a61aba0a81c26fb3caba --value 10 --gas 21001
// npm run start -- balance https://rpc.ankr.com/eth_goerli 0xcaeed488ad7e01286aa9a61aba0a81c26fb3caba
const commands = {
    'sendtx': sendtx,
    'balance': balance,
}

const commandsDesc = {
    'sendtx': '- sendtx node_url key address [--value value --gas gas_limit --nonce nonce]',
    'balance': '- balance node_url address',
}

function parseFlags(args, flags) {
    let i = 0
    while (i < args.length) {
        const x = args[i]
        if (!x.startsWith('--')) {
            i += 1
            continue
        }

        const fl = x.substring(2)
        if (fl in flags) {
            if (i + 1 < args.length) {
                flags[fl] = parseInt(args[i + 1])
            }
            i += 2
        } else {
            i += 1
        }
    }
}

async function sendtx(args) {
    if (args.length < 3) {
        console.log(commandsDesc['sendtx'])
        process.exit(1)
    }

    try {
        const web3 = new Web3(args[0])
        const gasPrice = await web3.eth.getGasPrice();
        const account = web3.eth.accounts.privateKeyToAccount(args[1])
        const address = args[2]
        const flags = { 'value': 1, 'gas': 21000, 'nonce': 0 }
        parseFlags(args.slice(3), flags)

        const tx = {
            from: account.address,
            to: address,
            value: flags['value'],
            gas: flags['gas'],
            gasPrice: gasPrice,
            nonce: flags['nonce']
        }

        // console.log(tx)

        const signedTx = await account.signTransaction(tx)
        const txHash = await web3.eth.sendSignedTransaction(signedTx.rawTransaction)
        console.log('Transaction hash:', txHash)
    } catch (err) {
        console.log('error: ', err)
        process.exit(1)
    }
}

async function balance(args) {
    if (args.length < 2) {
        console.log(commandsDesc['balance'])
        process.exit(1)
    }

    try {
        const web3 = new Web3(args[0])
        const address = args[1]
        const balance = await web3.eth.getBalance(address)
        console.log('Balance:', balance)
        fs.writeFileSync('/balance.txt', balance)
    } catch (err) {
        console.log('error: ', err)
        process.exit(1)
    }
}

const args = process.argv.slice(2)
if (args.length == 0 || !commands[args[0]]) {
    console.log(`
        ${commandsDesc['sendtx']}
        ${commandsDesc['balance']}
    `)
    process.exit(1)
}

commands[args[0]](args.slice(1))