/**
 * Script dịch tên và mô tả sản phẩm từ EN → VI
 * Dùng Google Translate unofficial API (không cần key)
 * Chạy: node translate_products.js
 */

const https = require('https');

const FIREBASE_URL = 'https://finalapp-c65a2-default-rtdb.firebaseio.com';

function translate(text) {
    return new Promise((resolve) => {
        if (!text || text.trim() === '') { resolve(''); return; }
        const encoded = encodeURIComponent(text.slice(0, 800));
        const path = `/translate_a/single?client=gtx&sl=en&tl=vi&dt=t&q=${encoded}`;
        const options = { hostname: 'translate.googleapis.com', path, method: 'GET' };
        const req = https.request(options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    // Ghép các đoạn dịch lại
                    const translated = json[0].map(part => part[0]).join('');
                    resolve(translated || text);
                } catch { resolve(text); }
            });
        });
        req.on('error', () => resolve(text));
        req.end();
    });
}

function firebaseGet(path) {
    return new Promise((resolve, reject) => {
        https.get(`${FIREBASE_URL}/${path}.json`, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => { try { resolve(JSON.parse(data)); } catch { reject(); } });
        }).on('error', reject);
    });
}

function firebasePatch(path, body) {
    return new Promise((resolve, reject) => {
        const payload = JSON.stringify(body);
        const urlObj = new URL(`${FIREBASE_URL}/${path}.json`);
        const options = {
            hostname: urlObj.hostname,
            path: urlObj.pathname,
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(payload) }
        };
        const req = https.request(options, (res) => {
            res.on('data', () => {});
            res.on('end', resolve);
        });
        req.on('error', reject);
        req.write(payload);
        req.end();
    });
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function main() {
    console.log('Đang tải danh sách sản phẩm từ Firebase...');
    const products = await firebaseGet('products');
    if (!products) { console.log('Không có sản phẩm nào.'); return; }

    const entries = Object.entries(products);
    console.log(`Tìm thấy ${entries.length} sản phẩm.\n`);

    let count = 0, skipped = 0, done = 0;
    for (const [id, product] of entries) {
        count++;

        const isBadTranslation = (val, original) =>
            !val || val.trim() === '' || val.includes('MYMEMORY') || val === original;

        const needName = isBadTranslation(product.nameVi, product.name);
        const needDesc = isBadTranslation(product.descriptionVi, product.description);

        if (!needName && !needDesc) {
            skipped++;
            process.stdout.write(`\r[${count}/${entries.length}] Bỏ qua: ${skipped} sp đã dịch...`);
            continue;
        }

        const update = {};
        if (needName) {
            update.nameVi = await translate(product.name || '');
            await sleep(150);
        }
        if (needDesc) {
            update.descriptionVi = await translate(product.description || '');
            await sleep(150);
        }

        await firebasePatch(`products/${id}`, update);
        done++;
        console.log(`[${count}/${entries.length}] ✓ ${product.name} → ${update.nameVi || product.nameVi}`);
        await sleep(100);
    }

    console.log(`\n✅ Hoàn thành! Đã dịch ${done} sản phẩm, bỏ qua ${skipped} sản phẩm đã có.`);
}

main().catch(err => console.error('Lỗi:', err));
