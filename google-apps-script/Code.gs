/**
 * Family Food Rations & Pantry Tracker — Google Apps Script Backend
 * Designed for a family of 2 adults + 3 kids (ages 9, 5, and <1) planning for 3 years.
 *
 * Compatible with Google Apps Script V8 runtime.
 * Connects cleanly to either an existing Google Sheet (smart auto-detection without
 * overwriting extra columns or formulas) or a brand-new blank spreadsheet.
 */

/* ==========================================================================
   CONSTANTS & CONFIGURATION
   ========================================================================== */

var BACKEND_VERSION = 'v5.1';
var PIN_LENGTH = 6;
var PIN_PROP_CODES = 'FAMILY_CODES_JSON';
var PIN_PROP_DEVICES = 'TRUSTED_DEVICES_JSON';
var MAX_TRUSTED_DEVICES = 25;
var DEVICE_LAST_SEEN_THROTTLE_MS = 5 * 60 * 1000;
var PIN_PROP_HASH = 'FAMILY_PIN_HASH';
var PIN_PROP_SALT = 'FAMILY_PIN_SALT';
var PIN_PROP_TOKEN = 'FAMILY_PIN_TOKEN';
var PIN_PROP_DISMISSED = 'FAMILY_PIN_DISMISSED';
var PIN_PROP_FAILS = 'FAMILY_PIN_FAILS';
var PIN_PROP_LOCK_UNTIL = 'FAMILY_PIN_LOCK_UNTIL';
var PIN_HASH_ROUNDS = 1000;
var INTERNAL_BYPASS_TOKEN_ = null;
var TG_PROP_BOT_TOKEN = 'TELEGRAM_BOT_TOKEN';
var TG_PROP_WEBHOOK_SECRET = 'TELEGRAM_WEBHOOK_SECRET';
var PANTRY_TRACKER_SOURCE_ID = '11ySbLHJaZi3eF_1iffbmlhhK1O_4o-7aYrnbkfgjd_0';
var BACKUP_SHEET_NAME = 'Backup Before Import';
var DEFAULT_INVENTORY_SHEET_NAME = 'Pantry Inventory';
var HISTORY_SHEET_NAME = 'History Log';
var BATCHES_SHEET_NAME = 'Batches';
var BATCHES_HEADERS = ['Batch ID', 'Item ID', 'Item Name', 'Qty', 'Expiration Date', 'Added At'];
var GROCERIES_SHEET_NAME = 'Groceries';
var GROCERIES_HEADERS = ['ID', 'Name', 'Qty', 'Unit', 'Aisle', 'Store', 'Checked', 'Favorite', 'Added By', 'Updated At'];
var SETTINGS_SHEET_NAME = 'Settings';
var SETTINGS_KEY = 'app_settings_json';
var SETTINGS_HEADERS = ['Key', 'Value', 'Updated'];
var VALID_WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
var HEADER_BG_COLOR = '#15803d';
var HEADER_TEXT_COLOR = '#ffffff';
var LOCK_TIMEOUT_MS = 10000;

var DEFAULT_CATEGORIES = [
  { id: 'grains',     name: 'Grains & Pasta',            color: '#d97706', soft: '#fef3c7', icon: '🌾' },
  { id: 'proteins',   name: 'Proteins & Lentils',        color: '#dc2626', soft: '#fee2e2', icon: '🫘' },
  { id: 'canned',     name: 'Canned Goods & Produce',    color: '#059669', soft: '#d1fae5', icon: '🥫' },
  { id: 'baby',       name: 'Baby & Kids (<1 to 3 yrs)', color: '#7c3aed', soft: '#ede9fe', icon: '🍼' },
  { id: 'water',      name: 'Water & Drinks',            color: '#2563eb', soft: '#dbeafe', icon: '💧' },
  { id: 'oils',       name: 'Oils, Spices & Cooking',    color: '#0d9488', soft: '#ccfbf1', icon: '🫒' },
  { id: 'essentials', name: 'Essentials & First Aid',    color: '#e11d48', soft: '#ffe4e6', icon: '🩹' }
];

var CUSTOM_CATEGORY_PALETTE = [
  { color: '#4f46e5', soft: '#e0e7ff', icon: '📦' },
  { color: '#0284c7', soft: '#e0f2fe', icon: '🧺' },
  { color: '#9333ea', soft: '#f3e8ff', icon: '🥣' },
  { color: '#ea580c', soft: '#ffedd5', icon: '🍎' },
  { color: '#65a30d', soft: '#ecfccb', icon: '🥗' },
  { color: '#db2777', soft: '#fce7f3', icon: '🧁' },
  { color: '#0891b2', soft: '#cffafe', icon: '🧊' },
  { color: '#b45309', soft: '#fef3c7', icon: '🍞' },
  { color: '#475569', soft: '#f1f5f9', icon: '🧰' },
  { color: '#16a34a', soft: '#dcfce7', icon: '🌿' }
];

var STANDARD_UNITS = [
  'lbs', 'kg', 'cans', 'bags', 'gallons', 'boxes', 'packs', 'jars', 'bottles', 'pouches'
];

var STANDARD_LOCATIONS = [
  'Main Pantry',
  'Basement Shelf',
  'Garage Bins',
  'Kitchen Cabinet',
  'Deep Freezer',
  'Closet / Hallway'
];

var SKIPPED_SHEET_NAMES = {
  'history log': true,
  'activity log': true,
  'summary': true,
  'dashboard': true,
  'instructions': true,
  'readme': true,
  'settings': true,
  'backup before import': true,
  'batches': true,
  'groceries': true
};

var CANONICAL_HEADERS = [
  { key: 'id',          title: 'ID',              width: 135, align: 'left' },
  { key: 'category',    title: 'Category',        width: 210, align: 'left' },
  { key: 'name',        title: 'Item Name',       width: 230, align: 'left' },
  { key: 'qty',         title: 'Current Stock',   width: 115, align: 'center' },
  { key: 'target',      title: '3-Year Goal',     width: 110, align: 'center' },
  { key: 'unit',        title: 'Unit',            width: 90,  align: 'center' },
  { key: 'expiry',      title: 'Expiration Date', width: 135, align: 'center' },
  { key: 'location',    title: 'Storage Spot',    width: 155, align: 'left' },
  { key: 'notes',       title: 'Notes',           width: 280, align: 'left' },
  { key: 'lastUpdated', title: 'Last Updated',    width: 160, align: 'center' }
];

var HISTORY_HEADERS = [
  'Timestamp',
  'Action',
  'Category',
  'Item Name',
  'Change',
  'New Stock',
  'Unit',
  'Expiration Date',
  'Notes'
];

var MONTH_LOOKUP = {
  jan: 0, january: 0,
  feb: 1, february: 1,
  mar: 2, march: 2,
  apr: 3, april: 3,
  may: 4,
  jun: 5, june: 5,
  jul: 6, july: 6,
  aug: 7, august: 7,
  sep: 8, sept: 8, september: 8,
  oct: 9, october: 9,
  nov: 10, november: 10,
  dec: 11, december: 11
};

var MONTH_SHORT_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

/* ==========================================================================
   WEB APP ENTRY & SPREADSHEET MENU
   ========================================================================== */

/**
 * ONE-CLICK AUTHORIZATION & TEST ALERT (placed first so Apps Script selects it by default!).
 * Click "Run" once to approve Google Fi SMS (MailApp), Google Calendar, and Scheduled Alerts.
 */
function authorizeAndTestAlerts() {
  // Touch required services outside try/catch so Google immediately prompts for permissions if needed
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var cal = CalendarApp.getDefaultCalendar();
  var quota = MailApp.getRemainingDailyQuota();
  var triggers = ScriptApp.getProjectTriggers();
  try {
    if (typeof UrlFetchApp !== 'undefined' && UrlFetchApp && typeof UrlFetchApp.getRequest === 'function') {
      UrlFetchApp.getRequest('https://api.telegram.org');
    }
  } catch (urlScopeErr) {
    // Ignore offline/mock issues while keeping script.external_request scope declared
  }
  Logger.log('✅ Authorized! Sheet: ' + ss.getName() + ' | Calendar: ' + cal.getName() + ' | Mail quota: ' + quota + ' | Triggers: ' + triggers.length);

  if (isPinSet_()) {
    var lockedAuthMsg = 'Authorized! Open the Web App Settings -> Notify to send a test alert.';
    Logger.log(lockedAuthMsg);
    return {
      ok: true,
      version: BACKEND_VERSION,
      message: lockedAuthMsg
    };
  }

  var result = sendMenuTestNotificationInternal_();
  Logger.log('🔔 Test Alert Result: ' + JSON.stringify(result));
  return result;
}

/**
 * Serves the mobile-friendly Family Pantry web app UI.
 */
function doGet(e) {
  return HtmlService.createHtmlOutputFromFile('Index')
    .setTitle('Family Pantry · Rations Tracker')
    .addMetaTag('viewport', 'width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover')
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.DEFAULT);
}

/**
 * Adds the custom Family Pantry menu when the spreadsheet opens.
 */
function onOpen() {
  try {
    SpreadsheetApp.getUi()
      .createMenu('🏠 Family Pantry')
      .addItem('📥 Import Data from Pantry Tracker', 'menuImportFromPantryTracker')
      .addSeparator()
      .addItem('✨ Verify / Setup Pantry Sheet', 'menuSetupPantrySheet')
      .addItem('🗂️ Organize & Sort Sheet by Category', 'menuOrganizeExistingSheet')
      .addItem('🎨 Refresh Sheet Colors & Dates', 'menuRefreshSheetFormatting')
      .addSeparator()
      .addItem('🔔 Send Test Phone/Email Alert', 'menuSendTestNotification')
      .addItem('🔑 Clear / Reset Family PIN', 'menuResetFamilyPin')
      .addItem('📝 Create Optional Synced Google Form', 'createSyncedGoogleForm')
      .addToUi();
  } catch (err) {
    Logger.log('onOpen error: ' + (err && err.message ? err.message : err));
  }
}

/**
 * Menu action: 1-click smart import from the original "Family Food Rations & Pantry Tracker" spreadsheet.
 */
function menuImportFromPantryTracker() {
  requireSheetUiContext_('Import Data from Pantry Tracker');
  return importFromSpreadsheetById_(PANTRY_TRACKER_SOURCE_ID);
}

/**
 * Smart non-destructive import from another Google Spreadsheet by ID with
 * ironclad anti-doubling protection:
 * - Fuzzy duplicate detection (exact, parenthetical-stripped, singularized, sorted-word)
 * - NEVER adds quantities together; only fills blank fields on existing items
 * - Shows Preview-Before-Import YES/NO confirmation dialog before modifying any cell
 * - Saves a safety backup tab ('Backup Before Import') before writing
 * - Appends new items above any bottom Summary/Total row, sorts cleanly, and logs to History Log
 */
function importFromSpreadsheetById_(sourceSpreadsheetId, options) {
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();

    if (!sourceSpreadsheetId || !String(sourceSpreadsheetId).trim()) {
      var missingErr = '[' + BACKEND_VERSION + '] Missing source spreadsheet ID.';
      tryShowAlert_('📥 Import Error', missingErr);
      return { ok: false, error: missingErr };
    }

    if (ss.getId() === sourceSpreadsheetId) {
      var sameMsg = 'You are already inside Family Food Rations & Pantry Tracker! Open Emergency Food Rations Manager to import into it.';
      tryShowAlert_('📥 Already in Source Sheet', sameMsg);
      return { ok: false, error: '[' + BACKEND_VERSION + '] ' + sameMsg };
    }

    var sourceSs = SpreadsheetApp.openById(sourceSpreadsheetId);
    var sourceInfo = findOrCreateInventorySheet_(sourceSs, false);
    if (sourceInfo.isNewSheet) {
      var tabNames = [];
      var srcSheets = sourceSs.getSheets();
      for (var t = 0; t < srcSheets.length; t++) {
        tabNames.push('"' + srcSheets[t].getName() + '"');
      }
      var noMatchMsg =
        'Could not auto-detect an inventory header row in "' + sourceSs.getName() + '".\n' +
        'Tabs found: ' + (tabNames.length ? tabNames.join(', ') : '(none)');
      tryShowAlert_('📥 No Inventory Tab Detected', noMatchMsg);
      return { ok: false, error: '[' + BACKEND_VERSION + '] ' + noMatchMsg };
    }

    var sourceParsed = readSheetInventory_(sourceSs, sourceInfo);

    // Read destination non-destructively FIRST so cancelling leaves 0 writes
    var destInfoPreview = findOrCreateInventorySheet_(ss, false);
    var destParsed = destInfoPreview.isNewSheet
      ? { categories: readSettingsInternal_(ss).categories, categoryMap: {}, items: [] }
      : readSheetInventory_(ss, destInfoPreview);

    // Register every existing destination item under all its fuzzy match keys (>= 3 chars)
    var destByKey = {};
    function registerDestItemKeys_(itemObj) {
      var keys = buildFuzzyItemKeys_(itemObj.name);
      for (var k = 0; k < keys.length; k++) {
        if (!destByKey[keys[k]]) {
          destByKey[keys[k]] = itemObj;
        }
      }
    }
    function findMatchingDestItem_(name) {
      var keys = buildFuzzyItemKeys_(name);
      for (var k = 0; k < keys.length; k++) {
        if (destByKey[keys[k]]) {
          return destByKey[keys[k]];
        }
      }
      return null;
    }

    for (var d = 0; d < destParsed.items.length; d++) {
      registerDestItemKeys_(destParsed.items[d]);
    }

    // Phase 1: Compute full merge plan BEFORE modifying a single cell
    var toAdd = [];
    var toEnrich = [];
    var toSkip = [];
    var plannedEnrichMap = {};

    for (var i = 0; i < sourceParsed.items.length; i++) {
      var srcItem = sourceParsed.items[i];
      if (!normalizeKey_(srcItem.name)) continue;

      var srcCatObj = sourceParsed.categoryMap[srcItem.category] ||
        destParsed.categoryMap[srcItem.category] ||
        DEFAULT_CATEGORIES[0];

      var existing = findMatchingDestItem_(srcItem.name);
      if (existing) {
        // Existing item (or already planned to be added in this import)
        if (existing._isPlannedAdd) {
          toSkip.push(srcItem);
          continue;
        }

        // Check only blank fields — NEVER add quantities together or overwrite non-zero qty/target!
        var fills = {};
        var hasFill = false;
        if (existing.qty === 0 && srcItem.qty > 0 && destInfoPreview.colMap.qty) {
          fills.qty = srcItem.qty;
          existing.qty = srcItem.qty;
          hasFill = true;
        }
        if (existing.target === 0 && srcItem.target > 0 && destInfoPreview.colMap.target) {
          fills.target = srcItem.target;
          existing.target = srcItem.target;
          hasFill = true;
        }
        if (existing.expiry === null && srcItem.expiry !== null && destInfoPreview.colMap.expiry) {
          fills.expiry = srcItem.expiry;
          existing.expiry = srcItem.expiry;
          hasFill = true;
        }
        if (existing.location === '' && srcItem.location !== '' && destInfoPreview.colMap.location) {
          fills.location = srcItem.location;
          existing.location = srcItem.location;
          hasFill = true;
        }
        if (existing.notes === '' && srcItem.notes !== '' && destInfoPreview.colMap.notes) {
          fills.notes = srcItem.notes;
          existing.notes = srcItem.notes;
          hasFill = true;
        }
        if (existing.unit === '' && srcItem.unit !== '' && destInfoPreview.colMap.unit) {
          fills.unit = srcItem.unit;
          existing.unit = srcItem.unit;
          hasFill = true;
        }

        if (hasFill) {
          if (!plannedEnrichMap[existing._rowIndex]) {
            var enrichPlan = { existing: existing, fills: fills };
            plannedEnrichMap[existing._rowIndex] = enrichPlan;
            toEnrich.push(enrichPlan);
          } else {
            var prevFills = plannedEnrichMap[existing._rowIndex].fills;
            var fKeys = Object.keys(fills);
            for (var fk = 0; fk < fKeys.length; fk++) {
              prevFills[fKeys[fk]] = fills[fKeys[fk]];
            }
          }
        } else {
          toSkip.push(srcItem);
        }
      } else {
        var plannedEntry = {
          srcItem: srcItem,
          catObj: srcCatObj,
          name: srcItem.name,
          _isPlannedAdd: true
        };
        toAdd.push(plannedEntry);
        registerDestItemKeys_(plannedEntry);
      }
    }

    // Short-circuit cleanly if nothing new to add or enrich
    if (toAdd.length === 0 && toEnrich.length === 0) {
      var upToDateMsg =
        'Everything is already in your sheet! Found ' + toSkip.length +
        ' matching duplicate(s) and 0 new items — nothing was changed or doubled.';
      tryShowAlert_('📥 Already Up to Date', upToDateMsg);
      return {
        ok: true,
        version: BACKEND_VERSION,
        addedCount: 0,
        updatedCount: 0,
        skippedCount: toSkip.length,
        message: upToDateMsg
      };
    }

    // Phase 2: Preview-Before-Import YES/NO Confirmation Dialog
    var previewLines = ['Preview Import from ' + sourceSs.getName() + ':', ''];
    if (toAdd.length > 0) {
      var shownNames = [];
      for (var p = 0; p < Math.min(toAdd.length, 12); p++) {
        shownNames.push(toAdd[p].srcItem.name);
      }
      var nameSummary = shownNames.join(', ');
      if (toAdd.length > 12) {
        nameSummary += ', and ' + (toAdd.length - 12) + ' more';
      }
      previewLines.push('• ' + toAdd.length + ' NEW item(s) to add:');
      previewLines.push('  - ' + nameSummary);
    } else {
      previewLines.push('• 0 NEW items to add');
    }
    previewLines.push('• ' + toEnrich.length + ' existing item(s) to fill missing blank fields (quantities are NEVER added together)');
    previewLines.push('• ' + toSkip.length + ' duplicate(s) will be skipped');
    previewLines.push('');
    previewLines.push("A safety copy ('" + BACKUP_SHEET_NAME + "') will be saved first. Proceed?");

    var previewMessage = previewLines.join('\n');
    var shouldSkipConfirm = (options === true) || Boolean(options && options.skipConfirm === true);
    var confirmed = shouldSkipConfirm
      ? true
      : tryConfirmYesNo_('📥 Confirm Import Preview', previewMessage);

    if (!confirmed) {
      return {
        ok: true,
        version: BACKEND_VERSION,
        cancelled: true,
        addedCount: 0,
        updatedCount: 0,
        skippedCount: toSkip.length,
        message: 'Import cancelled — no changes were made.'
      };
    }

    // Phase 3: Create safety backup tab BEFORE writing any imported rows
    var destInfo = destInfoPreview.isNewSheet
      ? findOrCreateInventorySheet_(ss, true)
      : destInfoPreview;

    if (!destInfoPreview.isNewSheet && destInfo.sheet) {
      try {
        var oldBackup = ss.getSheetByName(BACKUP_SHEET_NAME);
        if (oldBackup) {
          ss.deleteSheet(oldBackup);
        }
        var backupCopy = destInfo.sheet.copyTo(ss);
        backupCopy.setName(BACKUP_SHEET_NAME);
        SpreadsheetApp.flush();
      } catch (backupErr) {
        // Never allow backup copy errors to block a confirmed import
      }
    }

    // Phase 4: Apply blank-field fills first, then append new items cleanly
    var now = new Date();
    var updatedCount = 0;
    for (var e = 0; e < toEnrich.length; e++) {
      var enrichItem = toEnrich[e];
      var rowIdx = enrichItem.existing._rowIndex;
      var f = enrichItem.fills;
      if (f.qty !== undefined) writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.qty, f.qty);
      if (f.target !== undefined) writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.target, f.target);
      if (f.expiry !== undefined) writeExpiryCell_(destInfo.sheet, rowIdx, destInfo.colMap.expiry, f.expiry);
      if (f.location !== undefined) writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.location, f.location);
      if (f.notes !== undefined) writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.notes, f.notes);
      if (f.unit !== undefined) writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.unit, f.unit);
      writeCellIfMapped_(destInfo.sheet, rowIdx, destInfo.colMap.lastUpdated, now);
      updatedCount++;
    }

    var addedCount = 0;
    var styledAddedRows = [];
    for (var a = 0; a < toAdd.length; a++) {
      var addPlan = toAdd[a];
      var addSrc = addPlan.srcItem;
      var addCat = addPlan.catObj;
      var nextRow = getNextAppendRow_(destInfo);
      var newId = generateUniqueItemId_(addSrc.name, destParsed.items);

      writeItemRowValues_(destInfo.sheet, destInfo.colMap, nextRow, {
        id: newId,
        categoryName: addCat.name,
        name: addSrc.name,
        qty: addSrc.qty,
        target: addSrc.target,
        unit: addSrc.unit,
        expiry: addSrc.expiry,
        location: addSrc.location,
        notes: addSrc.notes,
        lastUpdated: now
      });

      destParsed.items.push({ id: newId, name: addSrc.name });
      styledAddedRows.push({ rowIndex: nextRow, catObj: addCat, expiry: addSrc.expiry });
      addedCount++;
    }

    SpreadsheetApp.flush();

    if (styledAddedRows.length > 0 && destInfo.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        for (var r = 0; r < styledAddedRows.length; r++) {
          formatInventoryRow_(destInfo.sheet, destInfo.colMap, styledAddedRows[r].rowIndex, styledAddedRows[r].catObj, styledAddedRows[r].expiry, tz);
        }
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    organizeInventorySheet_(ss, destInfo);
    appendHistoryLog_(ss, {
      timestamp: now,
      action: 'Imported Data',
      categoryName: 'All Categories',
      itemName: sourceSs.getName(),
      changeText: '+' + addedCount + ' new / ' + updatedCount + ' updated',
      newStock: destParsed.items.length,
      unit: 'items',
      expiry: null,
      notes: 'Imported from "' + sourceSs.getName() + '" (' + sourceInfo.sheet.getName() + '); skipped ' + toSkip.length + ' duplicate(s)'
    });

    var summaryPopup =
      'Import Complete!\n\n' +
      'Source: ' + sourceSs.getName() + ' (' + sourceInfo.sheet.getName() + ')\n' +
      '• Added ' + addedCount + ' new item(s)\n' +
      '• Filled missing details on ' + updatedCount + ' existing item(s)\n' +
      '• Skipped ' + toSkip.length + ' already-present duplicate(s)\n\n' +
      "A safety copy was saved in '" + BACKUP_SHEET_NAME + "', and your sheet has been sorted by Category & Item Name. Once you verify everything looks good, it's safe to delete the old Pantry Tracker sheet!";

    tryShowAlert_('📥 Import Complete!', summaryPopup);
    return {
      ok: true,
      version: BACKEND_VERSION,
      addedCount: addedCount,
      updatedCount: updatedCount,
      skippedCount: toSkip.length,
      message: summaryPopup
    };
  } catch (err) {
    var errMsg = '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err);
    tryShowAlert_('📥 Import Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Generates normalized fuzzy lookup keys for an item name to prevent duplicates
 * across parentheticals, punctuation, singular/plural variations, and word order.
 */
function buildFuzzyItemKeys_(name) {
  var keys = [];
  var seen = {};
  function addKey(k) {
    if (k && k.length >= 3 && !seen[k]) {
      seen[k] = true;
      keys.push(k);
    }
  }

  var exact = normalizeKey_(name);
  addKey(exact);

  var noPunctFull = exact.replace(/[^a-z0-9\s]/g, ' ').replace(/\s+/g, ' ').trim();
  addKey(noPunctFull);

  var withoutParens = exact
    .replace(/\([^)]*\)/g, ' ')
    .replace(/\[[^\]]*\]/g, ' ')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  addKey(withoutParens);

  var variants = withoutParens ? [withoutParens] : [];
  if (noPunctFull && noPunctFull !== withoutParens) {
    variants.push(noPunctFull);
  }
  for (var v = 0; v < variants.length; v++) {
    var tokens = variants[v].split(' ');
    var singularTokens = [];
    for (var t = 0; t < tokens.length; t++) {
      if (tokens[t]) {
        singularTokens.push(singularizeToken_(tokens[t]));
      }
    }
    if (singularTokens.length === 0) continue;
    addKey(singularTokens.join(' '));
    addKey(singularTokens.slice().sort().join(' '));
  }

  return keys;
}

function singularizeToken_(word) {
  var w = String(word || '').toLowerCase();
  if (w.length <= 3) return w;
  if (/ies$/.test(w) && w.length > 4) return w.slice(0, -3) + 'y';
  if (/(?:ch|sh|x|z|o)es$/.test(w) && w.length > 4) return w.slice(0, -2);
  if (/s$/.test(w) && !/(?:ss|us|is)$/.test(w)) return w.slice(0, -1);
  return w;
}

function tryConfirmYesNo_(title, message) {
  try {
    var ui = SpreadsheetApp.getUi();
    var response = ui.alert(title, message, ui.ButtonSet.YES_NO);
    return response === ui.Button.YES;
  } catch (e) {
    // Fail closed whenever Spreadsheet UI is unavailable
    return false;
  }
}

/**
 * Menu action: sends a test Google Fi SMS, Google Calendar Pop-Up, and/or Gmail alert.
 * If no SMS or Gmail channel is configured yet, defaults to Calendar Pop-Up + signed-in Gmail
 * so clicking this menu button authorizes BOTH CalendarApp and MailApp in a single click.
 */
function menuSendTestNotification() {
  requireSheetUiContext_('Send Test Phone/Email Alert');
  return sendMenuTestNotificationInternal_();
}

function sendMenuTestNotificationInternal_() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var saved = readSettingsInternal_(ss);
  var hasFiPhones = parseGoogleFiAddresses_(saved.notifyFiPhones).length > 0;
  var hasEmail = Boolean(String(saved.notifyEmail || '').trim());
  var hasGmailEnabled = Boolean(saved.notifyEmailEnabled && hasEmail);
  var hasCalendarEnabled = saved.notifyCalendar === true;

  var overrideCfg = null;
  var usedAutoFallback = false;
  var activeEmail = '';

  if ((!hasFiPhones && !hasGmailEnabled && !hasEmail) || (!hasFiPhones && !hasCalendarEnabled && !hasGmailEnabled)) {
    try {
      activeEmail = Session.getActiveUser().getEmail() || Session.getEffectiveUser().getEmail() || '';
    } catch (e) {
      activeEmail = '';
    }
    overrideCfg = {
      notifyCalendar: true,
      notifyEmailEnabled: Boolean(activeEmail),
      notifyEmail: activeEmail || saved.notifyEmail
    };
    usedAutoFallback = true;
  }

  var res = sendTestNotification(overrideCfg, getInternalPinToken_());
  if (res && res.ok) {
    var msg = res.message;
    if (usedAutoFallback) {
      msg += '\n\nOpen the mobile app → ⚙️ Settings → Notify to add your Google Fi phone number(s), invite your wife’s Gmail to Calendar reminders, or toggle Gmail summaries.';
      res.message = msg;
    }
    tryShowAlert_('🔔 Test Alert Sent!', msg);
  } else {
    tryShowAlert_('🔔 Test Alert Error', (res && res.error) ? res.error : 'Could not send test alert.');
  }
  return res;
}

/**
 * Menu action: sorts data rows by Category & Item Name (preserving bottom Total rows,
 * Google Sheets Tables, and typed columns), freezes header row, and refreshes colors.
 */
function menuOrganizeExistingSheet() {
  requireSheetUiContext_('Organize & Sort Sheet');
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, true);

    organizeInventorySheet_(ss, info);

    try {
      info.sheet.setFrozenRows(info.headerRow);
      SpreadsheetApp.flush();
    } catch (freezeErr) {
      // Ignore freeze errors on protected/locked sheets
    }

    var parsed = readSheetInventory_(ss, info);

    try {
      formatAllInventoryRows_(ss, info, parsed.categoryMap);
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Gracefully handle Google Sheets Tables / locked formatting
    }

    var summaryMsg = 'Organized ' + parsed.items.length + ' items — sorted by Category & Item Name!';
    tryShowAlert_('🗂️ Sheet Organized!', summaryMsg);
    return {
      ok: true,
      version: BACKEND_VERSION,
      message: summaryMsg,
      itemCount: parsed.items.length
    };
  } catch (err) {
    var errMsg = '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err);
    tryShowAlert_('Organize Sheet Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Menu action: verifies or sets up the Pantry Inventory and History Log sheets.
 */
function menuSetupPantrySheet() {
  requireSheetUiContext_('Verify / Setup Pantry Sheet');
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, true);
    ensureHistorySheet_(ss);
    var parsed = readSheetInventory_(ss, info);

    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        formatAllInventoryRows_(ss, info, parsed.categoryMap);
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore styling errors on Google Sheets Tables
      }
    }

    var msg =
      'Connected tab: "' + info.sheet.getName() + '"\n' +
      'Header row: Row ' + info.headerRow + '\n' +
      'Items detected: ' + parsed.items.length + '\n' +
      'Categories available: ' + parsed.categories.length + '\n' +
      'Backend version: ' + BACKEND_VERSION + '\n\n' +
      'Your sheet and "' + HISTORY_SHEET_NAME + '" tab are ready (existing columns and Tables are preserved untouched)!';

    tryShowAlert_('🏠 Family Pantry Setup Complete', msg);
    return {
      ok: true,
      version: BACKEND_VERSION,
      message: 'Verified sheet "' + info.sheet.getName() + '" (' + parsed.items.length + ' items).',
      sheetName: info.sheet.getName(),
      itemCount: parsed.items.length
    };
  } catch (err) {
    var errMsg = '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err);
    tryShowAlert_('Family Pantry Setup Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Menu action: refreshes category pill colors and row styling (safely skips locked Table cells).
 */
function menuRefreshSheetFormatting() {
  requireSheetUiContext_('Refresh Sheet Formatting');
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    try {
      formatAllInventoryRows_(ss, info, parsed.categoryMap);
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Gracefully handle Google Sheets Tables / locked formatting
    }

    tryShowAlert_(
      '🎨 Formatting Refreshed',
      'Updated category colors and row styling across ' +
        parsed.items.length + ' items in "' + info.sheet.getName() + '".'
    );
    return {
      ok: true,
      version: BACKEND_VERSION,
      message: 'Refreshed formatting for ' + parsed.items.length + ' items.'
    };
  } catch (err) {
    var errMsg = '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err);
    tryShowAlert_('Formatting Refresh Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Menu action: clears/resets all 6-digit Family Access Codes and trusted devices in 1 click
 * so the Sheet owner can immediately unlock the web app and optionally set new codes later.
 */
function menuResetFamilyPin() {
  requireSheetUiContext_('Clear / Reset Family PIN');
  var lock = null;
  try {
    lock = acquireScriptLock_();
    var props = getScriptProps_();
    if (props) {
      props.deleteProperty(PIN_PROP_CODES);
      props.deleteProperty(PIN_PROP_DEVICES);
      props.deleteProperty(PIN_PROP_HASH);
      props.deleteProperty(PIN_PROP_SALT);
      props.deleteProperty(PIN_PROP_TOKEN);
      props.deleteProperty(PIN_PROP_DISMISSED);
      props.deleteProperty(PIN_PROP_FAILS);
      props.deleteProperty(PIN_PROP_LOCK_UNTIL);
    }
    var msg = 'All Family Access Codes and trusted devices have been cleared! Your Family Pantry web app is now unlocked. You can set a new 6-digit access code anytime in Settings.';
    tryShowAlert_('🔑 Family PIN Cleared', msg);
    return {
      ok: true,
      version: BACKEND_VERSION,
      message: msg
    };
  } catch (err) {
    var errMsg = '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err);
    tryShowAlert_('🔑 Reset Family PIN Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/* ==========================================================================
   FAMILY PIN & SECRET SECURITY HELPERS (SCRIPT PROPERTIES ONLY — NEVER IN SHEET)
   ========================================================================== */

function requireSheetUiContext_(actionName) {
  try {
    var ui = SpreadsheetApp.getUi();
    if (!ui) {
      throw new Error('Spreadsheet UI unavailable');
    }
    return ui;
  } catch (e) {
    throw new Error('[' + BACKEND_VERSION + '] ' + (actionName || 'This action') + ' can only be run from the Google Sheet menu.');
  }
}

function acquireScriptLock_() {
  if (typeof LockService === 'undefined' || !LockService) return null;
  var lock = typeof LockService.getScriptLock === 'function'
    ? LockService.getScriptLock()
    : (typeof LockService.getDocumentLock === 'function' ? LockService.getDocumentLock() : null);
  if (lock && typeof lock.waitLock === 'function') {
    lock.waitLock(LOCK_TIMEOUT_MS);
  }
  return lock;
}

function setPropsAtomic_(props, map) {
  if (!props || !map) return;
  if (typeof props.setProperties === 'function') {
    props.setProperties(map, false);
    return;
  }
  var keys = Object.keys(map);
  for (var i = 0; i < keys.length; i++) {
    props.setProperty(keys[i], map[keys[i]]);
  }
}

function getScriptProps_() {
  if (typeof PropertiesService === 'undefined' || !PropertiesService || typeof PropertiesService.getScriptProperties !== 'function') {
    return null;
  }
  return PropertiesService.getScriptProperties();
}

function bytesToHex_(bytes) {
  var hex = [];
  for (var i = 0; i < bytes.length; i++) {
    var b = bytes[i] & 0xff;
    hex.push((b < 16 ? '0' : '') + b.toString(16));
  }
  return hex.join('');
}

function hashPin_(pin, salt) {
  var cleanSalt = String(salt === null || salt === undefined ? '' : salt);
  var cleanPin = String(pin === null || pin === undefined ? '' : pin);
  var useHmac = typeof Utilities !== 'undefined' && Utilities && typeof Utilities.computeHmacSha256Signature === 'function';
  var acc = cleanSalt + ':' + cleanPin;
  for (var iter = 0; iter < PIN_HASH_ROUNDS; iter++) {
    var payload = cleanSalt + ':' + iter + ':' + acc;
    var digest = useHmac
      ? Utilities.computeHmacSha256Signature(payload, cleanSalt)
      : Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, payload);
    acc = bytesToHex_(digest);
  }
  return acc;
}

function constantTimeEq_(a, b) {
  var sa = String(a === null || a === undefined ? '' : a);
  var sb = String(b === null || b === undefined ? '' : b);
  var len = Math.max(sa.length, sb.length);
  var diff = sa.length ^ sb.length;
  for (var i = 0; i < len; i++) {
    var ca = i < sa.length ? sa.charCodeAt(i) : 0;
    var cb = i < sb.length ? sb.charCodeAt(i) : 0;
    diff |= (ca ^ cb);
  }
  return diff === 0;
}

function makeUniquePrefixedId_(prefix, existingList) {
  var used = {};
  if (Array.isArray(existingList)) {
    for (var i = 0; i < existingList.length; i++) {
      if (existingList[i] && existingList[i].id) {
        used[existingList[i].id] = true;
      }
    }
  }
  var raw = (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.getUuid === 'function')
    ? String(Utilities.getUuid())
    : Math.random().toString(36).slice(2, 10);
  var candidate = prefix + raw.slice(0, 8);
  if (!used[candidate]) return candidate;
  var tail = raw.replace(/[^a-zA-Z0-9]/g, '');
  candidate = prefix + (tail.slice(-12) || Math.random().toString(36).slice(2, 10));
  var n = 2;
  while (used[candidate]) {
    candidate = prefix + tail.slice(-8) + '-' + n;
    n++;
  }
  return candidate;
}

function sanitizeCodeLabel_(label) {
  return String(label === null || label === undefined ? '' : label)
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 40);
}

function sanitizeDeviceName_(name) {
  return String(name === null || name === undefined ? '' : name)
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 60);
}

function prefixActorNotes_(notes, actorLabel) {
  var cleanNotes = String(notes === null || notes === undefined ? '' : notes).trim();
  var cleanActor = sanitizeCodeLabel_(actorLabel);
  if (!cleanActor) return cleanNotes;
  var prefix = '[' + cleanActor + ']';
  if (cleanNotes.indexOf(prefix) === 0) return cleanNotes;
  return cleanNotes ? (prefix + ' ' + cleanNotes) : prefix;
}

function getActorLabelFromAuth_(auth) {
  if (!auth || !auth.device || !auth.device.codeLabel) return '';
  return sanitizeCodeLabel_(auth.device.codeLabel);
}

function readAccessCodes_() {
  var props = getScriptProps_();
  if (!props) return [];
  var rawJson = props.getProperty(PIN_PROP_CODES);
  if (rawJson) {
    try {
      var parsed = JSON.parse(rawJson);
      if (Array.isArray(parsed)) {
        var clean = [];
        for (var i = 0; i < parsed.length; i++) {
          var c = parsed[i];
          if (!c || typeof c !== 'object') continue;
          var cid = String(c.id || '').trim();
          var clabel = sanitizeCodeLabel_(c.label) || 'Family Code';
          var csalt = String(c.salt || '');
          var chash = String(c.hash || '');
          if (!cid || !csalt || !chash) continue;
          clean.push({
            id: cid,
            label: clabel,
            salt: csalt,
            hash: chash,
            createdAt: String(c.createdAt || '')
          });
        }
        return clean;
      }
    } catch (e) {
      // Fall through to legacy check if JSON was malformed
    }
  }

  // Backward compatibility for legacy v4.2 single 4-digit PIN
  var legacyHash = props.getProperty(PIN_PROP_HASH);
  var legacySalt = props.getProperty(PIN_PROP_SALT);
  if (legacyHash && legacySalt) {
    return [{
      id: 'legacy-pin',
      label: 'Family PIN (Legacy)',
      salt: legacySalt,
      hash: legacyHash,
      createdAt: '',
      isLegacy: true
    }];
  }
  return [];
}

function writeAccessCodes_(props, codes) {
  if (!props) return;
  if (!Array.isArray(codes) || codes.length === 0) {
    props.deleteProperty(PIN_PROP_CODES);
    return;
  }
  var clean = [];
  for (var i = 0; i < codes.length; i++) {
    var c = codes[i];
    if (!c || !c.id || !c.salt || !c.hash) continue;
    clean.push({
      id: String(c.id),
      label: sanitizeCodeLabel_(c.label) || 'Family Code',
      salt: String(c.salt),
      hash: String(c.hash),
      createdAt: String(c.createdAt || '')
    });
  }
  if (clean.length === 0) {
    props.deleteProperty(PIN_PROP_CODES);
  } else {
    props.setProperty(PIN_PROP_CODES, JSON.stringify(clean));
  }
}

function readTrustedDevices_() {
  var props = getScriptProps_();
  if (!props) return [];
  var rawJson = props.getProperty(PIN_PROP_DEVICES);
  if (rawJson) {
    try {
      var parsed = JSON.parse(rawJson);
      if (Array.isArray(parsed)) {
        var clean = [];
        for (var i = 0; i < parsed.length; i++) {
          var d = parsed[i];
          if (!d || typeof d !== 'object') continue;
          var did = String(d.id || '').trim();
          var dtoken = String(d.token || '').trim();
          if (!did || !dtoken) continue;
          clean.push({
            id: did,
            token: dtoken,
            deviceName: sanitizeDeviceName_(d.deviceName) || 'Phone',
            codeId: String(d.codeId || '').trim(),
            codeLabel: sanitizeCodeLabel_(d.codeLabel) || '',
            remembered: Boolean(d.remembered),
            createdAt: String(d.createdAt || ''),
            lastSeenAt: String(d.lastSeenAt || d.createdAt || '')
          });
        }
        return clean;
      }
    } catch (e) {
      // Fall through to legacy check
    }
  }

  // Backward compatibility for legacy v4.2 single token when FAMILY_CODES_JSON is not yet set
  if (!props.getProperty(PIN_PROP_CODES)) {
    var legacyToken = props.getProperty(PIN_PROP_TOKEN);
    var legacyHash = props.getProperty(PIN_PROP_HASH);
    if (legacyToken && legacyHash) {
      return [{
        id: 'legacy-device',
        token: legacyToken,
        deviceName: 'Trusted Device',
        codeId: 'legacy-pin',
        codeLabel: 'Family PIN (Legacy)',
        remembered: true,
        createdAt: '',
        lastSeenAt: '',
        isLegacy: true
      }];
    }
  }
  return [];
}

function trimTrustedDevices_(devices) {
  if (!Array.isArray(devices) || devices.length <= MAX_TRUSTED_DEVICES) {
    return Array.isArray(devices) ? devices : [];
  }
  var indexed = [];
  for (var i = 0; i < devices.length; i++) {
    var ts = Date.parse(devices[i].lastSeenAt || devices[i].createdAt || '') || 0;
    indexed.push({ idx: i, ts: ts });
  }
  indexed.sort(function (a, b) { return a.ts - b.ts || a.idx - b.idx; });
  var dropCount = indexed.length - MAX_TRUSTED_DEVICES;
  var dropSet = {};
  for (var d = 0; d < dropCount; d++) {
    dropSet[indexed[d].idx] = true;
  }
  var kept = [];
  for (var k = 0; k < devices.length; k++) {
    if (!dropSet[k]) kept.push(devices[k]);
  }
  return kept;
}

function writeTrustedDevices_(props, devices) {
  if (!props) return;
  var trimmed = trimTrustedDevices_(devices);
  var clean = [];
  for (var i = 0; i < trimmed.length; i++) {
    var d = trimmed[i];
    if (!d || !d.id || !d.token || d.isLegacy) continue;
    clean.push({
      id: String(d.id),
      token: String(d.token),
      deviceName: sanitizeDeviceName_(d.deviceName) || 'Phone',
      codeId: String(d.codeId || ''),
      codeLabel: sanitizeCodeLabel_(d.codeLabel) || '',
      remembered: Boolean(d.remembered),
      createdAt: String(d.createdAt || ''),
      lastSeenAt: String(d.lastSeenAt || d.createdAt || '')
    });
  }
  if (clean.length === 0) {
    props.deleteProperty(PIN_PROP_DEVICES);
  } else {
    props.setProperty(PIN_PROP_DEVICES, JSON.stringify(clean));
  }
}

function isPinSet_() {
  return readAccessCodes_().length > 0;
}

function resolvePinToken_(pinToken) {
  if (!isPinSet_()) {
    return { valid: true, device: null };
  }
  var cleanToken = String(pinToken === null || pinToken === undefined ? '' : pinToken).trim();
  if (!cleanToken) {
    return { valid: false, device: null };
  }

  // Internal server-to-server bypass token (created only inside verified doPost / Sheet menu flows)
  if (INTERNAL_BYPASS_TOKEN_ && constantTimeEq_(cleanToken, INTERNAL_BYPASS_TOKEN_)) {
    return { valid: true, device: null, isInternal: true };
  }

  var devices = readTrustedDevices_();
  var matched = null;
  for (var i = 0; i < devices.length; i++) {
    if (devices[i].token && constantTimeEq_(cleanToken, devices[i].token)) {
      matched = devices[i];
    }
  }
  if (!matched) {
    return { valid: false, device: null };
  }

  // Update lastSeenAt throttled (>= 5 min gap so reads don't write on every tap)
  if (!matched.isLegacy) {
    var nowMs = Date.now();
    var prevMs = Date.parse(matched.lastSeenAt || '');
    if (!isFinite(prevMs) || (nowMs - prevMs) >= DEVICE_LAST_SEEN_THROTTLE_MS) {
      var nowIso = new Date(nowMs).toISOString();
      matched.lastSeenAt = nowIso;
      try {
        var lock = (typeof LockService !== 'undefined' && LockService && typeof LockService.getScriptLock === 'function')
          ? LockService.getScriptLock()
          : null;
        var acquired = lock && typeof lock.tryLock === 'function' ? lock.tryLock(1000) : true;
        if (acquired) {
          try {
            var props = getScriptProps_();
            if (props) {
              var freshDevices = readTrustedDevices_();
              var updatedAny = false;
              for (var j = 0; j < freshDevices.length; j++) {
                if (freshDevices[j].id === matched.id && constantTimeEq_(cleanToken, freshDevices[j].token)) {
                  freshDevices[j].lastSeenAt = nowIso;
                  updatedAny = true;
                  break;
                }
              }
              if (updatedAny) {
                writeTrustedDevices_(props, freshDevices);
              }
            }
          } finally {
            if (lock && acquired) releaseLockSafe_(lock);
          }
        }
      } catch (touchErr) {
        // Non-fatal: never break a valid authenticated read if throttled timestamp write fails
      }
    }
  }

  return { valid: true, device: matched };
}

function isValidPinToken_(pinToken) {
  return resolvePinToken_(pinToken).valid;
}

function getInternalPinToken_() {
  var rand = (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.getUuid === 'function')
    ? Utilities.getUuid()
    : (String(Date.now()) + '-' + Math.random().toString(36).slice(2));
  INTERNAL_BYPASS_TOKEN_ = 'internal-' + rand;
  return INTERNAL_BYPASS_TOKEN_;
}

function buildSecuritySummary_(currentToken) {
  var cleanCurrent = String(currentToken === null || currentToken === undefined ? '' : currentToken).trim();
  var codes = readAccessCodes_();
  var devices = readTrustedDevices_();

  var publicCodes = [];
  for (var i = 0; i < codes.length; i++) {
    publicCodes.push({
      id: codes[i].id,
      label: codes[i].label,
      createdAt: codes[i].createdAt || ''
    });
  }

  var publicDevices = [];
  for (var d = 0; d < devices.length; d++) {
    var dev = devices[d];
    publicDevices.push({
      id: dev.id,
      deviceName: dev.deviceName || 'Phone',
      codeLabel: dev.codeLabel || '',
      remembered: Boolean(dev.remembered),
      createdAt: dev.createdAt || '',
      lastSeenAt: dev.lastSeenAt || '',
      isCurrent: Boolean(cleanCurrent && dev.token && constantTimeEq_(cleanCurrent, dev.token))
    });
  }

  return {
    pinLength: PIN_LENGTH,
    pinSet: publicCodes.length > 0,
    codes: publicCodes,
    devices: publicDevices
  };
}

function isPinPromptDismissed_() {
  var props = getScriptProps_();
  if (!props) return false;
  return props.getProperty(PIN_PROP_DISMISSED) === 'true';
}

function buildLockedResponse_() {
  return {
    ok: false,
    locked: true,
    error: '[' + BACKEND_VERSION + '] Locked: enter your 6-digit Family Access Code.'
  };
}

function getTelegramBotToken_(ss) {
  var props = getScriptProps_();
  var propToken = props ? String(props.getProperty(TG_PROP_BOT_TOKEN) || '').trim() : '';
  if (propToken) return propToken;
  // Read-only legacy fallback if an older Settings sheet still holds a token before migration
  if (ss) {
    try {
      var sheet = ss.getSheetByName(SETTINGS_SHEET_NAME);
      if (sheet && sheet.getLastRow() >= 1 && sheet.getLastColumn() >= 2) {
        var values = sheet.getRange(1, 1, sheet.getLastRow(), 2).getValues();
        for (var r = 0; r < values.length; r++) {
          if (String(values[r][0] || '').trim() === SETTINGS_KEY) {
            var jsonStr = String(values[r][1] || '').trim();
            if (!jsonStr) break;
            var parsed = JSON.parse(jsonStr);
            if (parsed && parsed.telegramBotToken) {
              return String(parsed.telegramBotToken).trim();
            }
            break;
          }
        }
      }
    } catch (e) {
      // Ignore parse errors
    }
  }
  return '';
}

function saveTelegramBotToken_(token) {
  var props = getScriptProps_();
  if (!props) return;
  var clean = String(token === null || token === undefined ? '' : token).trim();
  if (clean) {
    props.setProperty(TG_PROP_BOT_TOKEN, clean);
  } else {
    props.deleteProperty(TG_PROP_BOT_TOKEN);
  }
}

function getTelegramWebhookSecret_() {
  var props = getScriptProps_();
  if (!props) return '';
  return String(props.getProperty(TG_PROP_WEBHOOK_SECRET) || '').trim();
}

function sanitizeSettingsForClient_(settingsObj, ss) {
  var copy = normalizeSettingsBaseCopy_(settingsObj);
  var storedToken = String(copy.telegramBotToken || '').trim() || getTelegramBotToken_(ss);
  delete copy.telegramBotToken;
  copy.hasTelegramBotToken = Boolean(storedToken);
  return copy;
}

/* ==========================================================================
   PUBLIC WEB APP RPC ENDPOINTS
   ========================================================================== */

/**
 * Verifies a 6-digit Family Access Code against all configured codes in ScriptProperties.
 * Includes brute-force rate limiting (5/10/15 failed attempts -> 5/30/60 min lockout)
 * and per-device trust registration (max 25 trusted devices).
 */
function verifyFamilyPin(code6, deviceName, rememberDevice) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    var props = getScriptProps_();
    if (!isPinSet_()) {
      var ssOpen = SpreadsheetApp.getActiveSpreadsheet();
      return {
        ok: true,
        pinToken: '',
        codeLabel: '',
        data: readPantryDataInternal_(ssOpen, findOrCreateInventorySheet_(ssOpen, false), '')
      };
    }

    var nowMs = Date.now();
    var lockUntil = Number(props.getProperty(PIN_PROP_LOCK_UNTIL) || '0');
    if (isFinite(lockUntil) && nowMs < lockUntil) {
      var minsLeft = Math.max(1, Math.ceil((lockUntil - nowMs) / 60000));
      return {
        ok: false,
        locked: true,
        error: 'Too many failed access code attempts. Wait ' + minsLeft + ' min(s) or clear PIN from the Google Sheet menu.'
      };
    }

    var codes = readAccessCodes_();
    var isLegacyOnly = codes.length === 1 && codes[0].isLegacy === true;
    var cleanCode = String(code6 === null || code6 === undefined ? '' : code6).trim();
    if (!/^\d{6}$/.test(cleanCode) && !(isLegacyOnly && /^\d{4}$/.test(cleanCode))) {
      return { ok: false, error: 'Please enter a valid 6-digit access code.' };
    }

    var matched = null;
    for (var i = 0; i < codes.length; i++) {
      var candidateHash = hashPin_(cleanCode, codes[i].salt);
      if (constantTimeEq_(candidateHash, codes[i].hash)) {
        matched = codes[i];
      }
    }

    if (!matched) {
      var fails = Number(props.getProperty(PIN_PROP_FAILS) || '0') + 1;
      var failProps = {};
      failProps[PIN_PROP_FAILS] = String(fails);
      var lockMinutes = 0;
      if (fails >= 15) {
        lockMinutes = 60;
      } else if (fails >= 10) {
        lockMinutes = 30;
      } else if (fails >= 5) {
        lockMinutes = 5;
      }
      if (lockMinutes > 0) {
        failProps[PIN_PROP_LOCK_UNTIL] = String(Date.now() + lockMinutes * 60 * 1000);
      }
      setPropsAtomic_(props, failProps);
      if (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.sleep === 'function') {
        Utilities.sleep(500);
      }
      if (lockMinutes > 0) {
        return {
          ok: false,
          locked: true,
          error: 'Too many failed access code attempts. Wait ' + lockMinutes + ' min(s) or clear PIN from the Google Sheet menu.'
        };
      }
      return { ok: false, error: 'Incorrect 6-digit access code. Please try again.' };
    }

    props.deleteProperty(PIN_PROP_FAILS);
    props.deleteProperty(PIN_PROP_LOCK_UNTIL);

    var devices = readTrustedDevices_();
    var nonLegacyDevices = [];
    for (var d = 0; d < devices.length; d++) {
      if (!devices[d].isLegacy) {
        nonLegacyDevices.push(devices[d]);
      }
    }
    var deviceId = makeUniquePrefixedId_('dev-', nonLegacyDevices);
    var deviceToken = Utilities.getUuid();
    var cleanName = sanitizeDeviceName_(deviceName) || 'Phone';
    var nowIso = new Date().toISOString();
    nonLegacyDevices.push({
      id: deviceId,
      token: deviceToken,
      deviceName: cleanName,
      codeId: matched.id,
      codeLabel: matched.label,
      remembered: Boolean(rememberDevice),
      createdAt: nowIso,
      lastSeenAt: nowIso
    });
    writeTrustedDevices_(props, nonLegacyDevices);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    return {
      ok: true,
      pinToken: deviceToken,
      codeLabel: matched.label,
      deviceId: deviceId,
      data: readPantryDataInternal_(ss, findOrCreateInventorySheet_(ss, false), deviceToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Adds a named 6-digit access code (e.g., "Fawad", "Wife", "Family Backup").
 * Rejects duplicate 6-digit codes across existing codes. If this is the very first code,
 * creates a trusted device for the creator and returns pinToken so they stay unlocked.
 */
function addAccessCode(label, code6, pinToken, currentDeviceName) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    if (isPinSet_() && !resolvePinToken_(pinToken).valid) {
      return buildLockedResponse_();
    }

    var cleanLabel = sanitizeCodeLabel_(label);
    if (!cleanLabel) {
      return { ok: false, error: 'Please enter a name for this access code (e.g. Fawad, Wife).' };
    }

    var cleanCode = String(code6 === null || code6 === undefined ? '' : code6).trim();
    if (!/^\d{6}$/.test(cleanCode)) {
      return { ok: false, error: 'Please enter a valid 6-digit numeric access code.' };
    }

    var props = getScriptProps_();
    if (!props) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Script properties service unavailable.' };
    }

    var existingCodes = readAccessCodes_();
    var wasEmpty = existingCodes.length === 0;
    var wasLegacyOnly = existingCodes.length === 1 && existingCodes[0].isLegacy === true;
    var activeCodes = wasLegacyOnly ? [] : existingCodes;

    // Reject duplicate 6-digit code across existing codes
    for (var i = 0; i < activeCodes.length; i++) {
      if (constantTimeEq_(hashPin_(cleanCode, activeCodes[i].salt), activeCodes[i].hash)) {
        return { ok: false, error: 'That 6-digit access code is already in use. Please choose a different code.' };
      }
    }

    var codeId = makeUniquePrefixedId_('code-', activeCodes);
    var salt = Utilities.getUuid();
    var hash = hashPin_(cleanCode, salt);
    var nowIso = new Date().toISOString();

    activeCodes.push({
      id: codeId,
      label: cleanLabel,
      salt: salt,
      hash: hash,
      createdAt: nowIso
    });
    writeAccessCodes_(props, activeCodes);

    // Clean up legacy properties once migrated to multi-code storage
    if (wasLegacyOnly) {
      props.deleteProperty(PIN_PROP_HASH);
      props.deleteProperty(PIN_PROP_SALT);
      props.deleteProperty(PIN_PROP_TOKEN);
    }
    props.deleteProperty(PIN_PROP_DISMISSED);
    props.deleteProperty(PIN_PROP_FAILS);
    props.deleteProperty(PIN_PROP_LOCK_UNTIL);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);

    if (wasEmpty || wasLegacyOnly) {
      var devices = readTrustedDevices_().filter(function (d) { return !d.isLegacy; });
      var deviceId = makeUniquePrefixedId_('dev-', devices);
      var newToken = Utilities.getUuid();
      var cleanDevName = sanitizeDeviceName_(currentDeviceName) || 'This Device';
      devices.push({
        id: deviceId,
        token: newToken,
        deviceName: cleanDevName,
        codeId: codeId,
        codeLabel: cleanLabel,
        remembered: true,
        createdAt: nowIso,
        lastSeenAt: nowIso
      });
      writeTrustedDevices_(props, devices);

      return {
        ok: true,
        codeId: codeId,
        pinToken: newToken,
        message: '6-digit access code "' + cleanLabel + '" added! Other phones will now ask for a code.',
        data: readPantryDataInternal_(ss, info, newToken)
      };
    }

    return {
      ok: true,
      codeId: codeId,
      message: '6-digit access code "' + cleanLabel + '" added!',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Removes an access code by ID and revokes all trusted devices that authenticated with it.
 * If no codes remain, clears TRUSTED_DEVICES_JSON and marks PIN_PROP_DISMISSED = 'true'.
 */
function removeAccessCode(codeId, pinToken) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    if (isPinSet_() && !resolvePinToken_(pinToken).valid) {
      return buildLockedResponse_();
    }

    var cleanId = String(codeId === null || codeId === undefined ? '' : codeId).trim();
    if (!cleanId) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Missing access code ID.' };
    }

    var props = getScriptProps_();
    if (!props) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Script properties service unavailable.' };
    }

    var codes = readAccessCodes_();
    var removedCode = null;
    var remainingCodes = [];
    for (var i = 0; i < codes.length; i++) {
      if (!removedCode && codes[i].id === cleanId) {
        removedCode = codes[i];
      } else if (!codes[i].isLegacy) {
        remainingCodes.push(codes[i]);
      }
    }

    if (!removedCode) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Access code not found.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);

    if (remainingCodes.length === 0) {
      var dismissMap = {};
      dismissMap[PIN_PROP_DISMISSED] = 'true';
      setPropsAtomic_(props, dismissMap);
      props.deleteProperty(PIN_PROP_CODES);
      props.deleteProperty(PIN_PROP_DEVICES);
      props.deleteProperty(PIN_PROP_HASH);
      props.deleteProperty(PIN_PROP_SALT);
      props.deleteProperty(PIN_PROP_TOKEN);
      props.deleteProperty(PIN_PROP_FAILS);
      props.deleteProperty(PIN_PROP_LOCK_UNTIL);

      return {
        ok: true,
        removedCodeId: cleanId,
        remainingCodesCount: 0,
        revokedSelf: false,
        message: 'Removed "' + removedCode.label + '". App lock is now turned off.',
        data: readPantryDataInternal_(ss, info, '')
      };
    }

    writeAccessCodes_(props, remainingCodes);

    var devices = readTrustedDevices_();
    var remainingDevices = [];
    var revokedCount = 0;
    var revokedSelf = false;
    var cleanToken = String(pinToken || '').trim();

    for (var d = 0; d < devices.length; d++) {
      if (devices[d].codeId === cleanId) {
        revokedCount++;
        if (cleanToken && devices[d].token && constantTimeEq_(cleanToken, devices[d].token)) {
          revokedSelf = true;
        }
      } else if (!devices[d].isLegacy) {
        remainingDevices.push(devices[d]);
      }
    }
    writeTrustedDevices_(props, remainingDevices);

    if (revokedSelf) {
      return {
        ok: true,
        removedCodeId: cleanId,
        remainingCodesCount: remainingCodes.length,
        revokedSelf: true,
        locked: true,
        message: 'Removed "' + removedCode.label + '". This device has been signed out.'
      };
    }

    return {
      ok: true,
      removedCodeId: cleanId,
      remainingCodesCount: remainingCodes.length,
      revokedSelf: false,
      message: 'Removed "' + removedCode.label + '"' + (revokedCount > 0 ? (' and signed out ' + revokedCount + ' device(s)') : '') + '.',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Revokes a single trusted device by ID. Returns revokedSelf: true if revoking the caller's device.
 */
function revokeTrustedDevice(deviceId, pinToken) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    if (isPinSet_() && !resolvePinToken_(pinToken).valid) {
      return buildLockedResponse_();
    }

    var cleanId = String(deviceId === null || deviceId === undefined ? '' : deviceId).trim();
    if (!cleanId) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Missing device ID.' };
    }

    var props = getScriptProps_();
    if (!props) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Script properties service unavailable.' };
    }

    var devices = readTrustedDevices_();
    var removedDevice = null;
    var remaining = [];
    for (var i = 0; i < devices.length; i++) {
      if (!removedDevice && devices[i].id === cleanId) {
        removedDevice = devices[i];
      } else if (!devices[i].isLegacy) {
        remaining.push(devices[i]);
      }
    }

    if (!removedDevice) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Trusted device not found.' };
    }

    if (removedDevice.isLegacy) {
      props.deleteProperty(PIN_PROP_TOKEN);
    } else {
      writeTrustedDevices_(props, remaining);
    }

    var cleanToken = String(pinToken || '').trim();
    var revokedSelf = Boolean(cleanToken && removedDevice.token && constantTimeEq_(cleanToken, removedDevice.token));

    if (revokedSelf) {
      return {
        ok: true,
        revokedSelf: true,
        locked: isPinSet_(),
        message: 'Signed out "' + (removedDevice.deviceName || 'Phone') + '".'
      };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      revokedSelf: false,
      message: 'Signed out "' + (removedDevice.deviceName || 'Phone') + '".',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Revokes all trusted devices EXCEPT the device matching pinToken.
 */
function revokeOtherDevices(pinToken) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    var auth = resolvePinToken_(pinToken);
    if (isPinSet_() && !auth.valid) {
      return buildLockedResponse_();
    }

    var props = getScriptProps_();
    if (!props) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Script properties service unavailable.' };
    }

    var cleanToken = String(pinToken || '').trim();
    var devices = readTrustedDevices_();
    var kept = [];
    var revokedCount = 0;

    for (var i = 0; i < devices.length; i++) {
      if (cleanToken && devices[i].token && constantTimeEq_(cleanToken, devices[i].token)) {
        if (!devices[i].isLegacy) {
          kept.push(devices[i]);
        }
      } else {
        revokedCount++;
      }
    }
    writeTrustedDevices_(props, kept);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      revokedCount: revokedCount,
      message: revokedCount > 0
        ? ('Signed out ' + revokedCount + ' other device' + (revokedCount === 1 ? '' : 's') + '.')
        : 'No other devices were signed in.',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Compatibility wrapper: adds a 6-digit Family access code.
 */
function setFamilyPin(newPin, currentPinToken) {
  return addAccessCode('Family Code', newPin, currentPinToken, 'This Device');
}

/**
 * Removes all Family access codes and trusted devices (requires valid currentPinToken if locked).
 * Atomically sets FAMILY_PIN_DISMISSED = 'true' so the setup banner does not reappear.
 */
function clearFamilyPin(currentPinToken) {
  var lock = null;
  try {
    lock = acquireScriptLock_();
    if (isPinSet_() && !resolvePinToken_(currentPinToken).valid) {
      return { ok: false, locked: true, error: 'Unlock with current access code first.' };
    }
    var props = getScriptProps_();
    if (props) {
      var dismissMap = {};
      dismissMap[PIN_PROP_DISMISSED] = 'true';
      setPropsAtomic_(props, dismissMap);
      props.deleteProperty(PIN_PROP_CODES);
      props.deleteProperty(PIN_PROP_DEVICES);
      props.deleteProperty(PIN_PROP_HASH);
      props.deleteProperty(PIN_PROP_SALT);
      props.deleteProperty(PIN_PROP_TOKEN);
      props.deleteProperty(PIN_PROP_FAILS);
      props.deleteProperty(PIN_PROP_LOCK_UNTIL);
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    return {
      ok: true,
      message: 'All Family access codes and trusted devices cleared.',
      data: readPantryDataInternal_(ss, findOrCreateInventorySheet_(ss, false), '')
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Marks the optional first-time PIN setup prompt as dismissed across devices.
 */
function dismissPinSetupPrompt(pinToken) {
  if (!resolvePinToken_(pinToken).valid) {
    return buildLockedResponse_();
  }
  try {
    var props = getScriptProps_();
    if (props) {
      props.setProperty(PIN_PROP_DISMISSED, 'true');
    }
    return {
      ok: true,
      pinPromptDismissed: true
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  }
}

/**
 * Reads all pantry categories, items, groceries, and security status from the connected spreadsheet.
 * 100% read-only on existing sheets (never mutates existing Tables or columns).
 * When access codes are active and pinToken is missing/invalid, returns locked state ONLY
 * without reading or leaking any spreadsheet, groceries, settings, or security data.
 */
function getPantryData(pinToken) {
  var lock = null;
  try {
    var auth = resolvePinToken_(pinToken);
    if (isPinSet_() && !auth.valid) {
      return {
        ok: true,
        version: BACKEND_VERSION,
        locked: true,
        pinSet: true,
        pinLength: PIN_LENGTH
      };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);

    // Only acquire lock and mutate when NO inventory tab exists yet (creating brand-new tab).
    if (info.isNewSheet) {
      lock = LockService.getDocumentLock();
      lock.waitLock(LOCK_TIMEOUT_MS);
      info = findOrCreateInventorySheet_(ss, true);
      return readPantryDataInternal_(ss, info, pinToken);
    }

    return readPantryDataInternal_(ss, info, pinToken);
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Saves an Add / Restock, Use / Take Out, or New Item form submission.
 * payload = {
 *   mode: 'add' | 'use' | 'new',
 *   itemId: string | null,
 *   category: string,
 *   name: string,
 *   qtyDelta: number,
 *   unit: string,
 *   target: number,
 *   expiry: { y, m, d } | null,
 *   location: string,
 *   notes: string,
 *   updateMeta: boolean
 * }
 */
function saveFormEntry(payload, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    return saveFormEntryInternal_(ss, payload, null, actorLabel, pinToken);
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Quickly increments or decrements an item's stock by delta (e.g. +1 or -1).
 * Automatically applies FIFO deduction across tracked batches when negative!
 */
function quickAdjustItem(itemId, delta, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!itemId || !String(itemId).trim()) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Missing item ID.' };
    }
    var numDelta = parseSignedNumber_(delta);
    if (numDelta === null || numDelta === 0) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Adjustment amount must be a non-zero number.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    var targetItem = findItemById_(parsed.items, String(itemId).trim());
    if (!targetItem) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Item not found in spreadsheet. Please refresh and try again.' };
    }

    var rowIndex = targetItem._rowIndex;
    var oldQty = targetItem.qty;
    var newQty = Math.max(0, roundQty_(oldQty + numDelta));
    var newExpiry = targetItem.expiry;
    var now = new Date();

    // Sync tracked FIFO batches if this item has rows in the Batches tab
    var batchIndex = readBatchesInternal_(ss);
    var tracked = getBatchesForItem_(batchIndex, targetItem);
    if (tracked.length > 0) {
      if (numDelta < 0) {
        var fifoOut = deductBatchesFifo_(tracked, Math.abs(numDelta));
        replaceItemBatches_(ss, targetItem.id, targetItem.name, fifoOut.remainingBatches);
        newQty = fifoOut.totalQty;
        newExpiry = fifoOut.soonestExpiry;
      } else {
        var addedOut = addOrMergeBatch_(tracked, numDelta, targetItem.expiry, now);
        replaceItemBatches_(ss, targetItem.id, targetItem.name, addedOut.batches);
        newQty = addedOut.totalQty;
        newExpiry = addedOut.soonestExpiry;
      }
      writeExpiryCell_(info.sheet, rowIndex, info.colMap.expiry, newExpiry);
    }

    var actualChange = roundQty_(newQty - oldQty);
    writeCellIfMapped_(info.sheet, rowIndex, info.colMap.qty, newQty);
    writeCellIfMapped_(info.sheet, rowIndex, info.colMap.lastUpdated, now);
    SpreadsheetApp.flush();

    var catObj = parsed.categoryMap[targetItem.category] || DEFAULT_CATEGORIES[0];
    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        formatInventoryRow_(info.sheet, info.colMap, rowIndex, catObj, newExpiry, tz);
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    var changeLabel = (actualChange >= 0 ? '+' : '') + actualChange;
    appendHistoryLog_(ss, {
      timestamp: now,
      action: numDelta > 0 ? 'Quick Add' : 'Quick Use',
      categoryName: catObj.name,
      itemName: targetItem.name,
      changeText: changeLabel,
      newStock: newQty,
      unit: targetItem.unit,
      expiry: newExpiry,
      notes: prefixActorNotes_(targetItem.notes, actorLabel)
    });

    var freshData = readPantryDataInternal_(ss, info, pinToken);
    var unitSuffix = targetItem.unit ? ' ' + targetItem.unit : '';
    var msg =
      (numDelta > 0 ? 'Added ' + Math.abs(numDelta) : 'Used ' + Math.abs(numDelta)) +
      unitSuffix + ' · "' + targetItem.name + '" now at ' + newQty + unitSuffix + '.';

    return { ok: true, message: msg, data: freshData };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Deletes an item row from the inventory sheet (plus any tracked Batches) and logs the deletion.
 */
function deleteItem(itemId, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!itemId || !String(itemId).trim()) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Missing item ID.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    var targetItem = findItemById_(parsed.items, String(itemId).trim());
    if (!targetItem) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Item not found in spreadsheet. It may already be deleted.' };
    }

    var catObj = parsed.categoryMap[targetItem.category] || DEFAULT_CATEGORIES[0];
    info.sheet.deleteRow(targetItem._rowIndex);
    SpreadsheetApp.flush();

    replaceItemBatches_(ss, targetItem.id, targetItem.name, []);

    appendHistoryLog_(ss, {
      timestamp: new Date(),
      action: 'Deleted',
      categoryName: catObj.name,
      itemName: targetItem.name,
      changeText: targetItem.qty > 0 ? '-' + targetItem.qty : '0',
      newStock: 0,
      unit: targetItem.unit,
      expiry: targetItem.expiry,
      notes: prefixActorNotes_('Removed item from pantry', actorLabel)
    });

    var freshData = readPantryDataInternal_(ss, info, pinToken);
    return {
      ok: true,
      message: 'Deleted "' + targetItem.name + '" from pantry.',
      data: freshData
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Discards a specific batch (e.g. expired batch) from an item, recalculates total stock
 * and next soonest expiry on the main sheet, and logs 'Discarded Batch' to History Log.
 */
function deleteBatch(itemId, batchId, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!itemId || !String(itemId).trim() || !batchId || !String(batchId).trim()) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Missing itemId or batchId.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    var targetItem = findItemById_(parsed.items, String(itemId).trim());
    if (!targetItem) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Item not found in spreadsheet.' };
    }

    var cleanBatchId = String(batchId).trim();
    var batchIndex = readBatchesInternal_(ss);
    var tracked = getBatchesForItem_(batchIndex, targetItem);
    var now = new Date();
    var removedBatch = null;
    var newQty = targetItem.qty;
    var newExpiry = targetItem.expiry;

    if (tracked.length > 0) {
      var remaining = [];
      for (var i = 0; i < tracked.length; i++) {
        if (!removedBatch && tracked[i].batchId === cleanBatchId) {
          removedBatch = tracked[i];
        } else {
          remaining.push(tracked[i]);
        }
      }
      if (!removedBatch) {
        return { ok: false, error: '[' + BACKEND_VERSION + '] Batch not found. Please refresh and try again.' };
      }
      sortBatchesFifo_(remaining);
      replaceItemBatches_(ss, targetItem.id, targetItem.name, remaining);
      newQty = sumBatchesQty_(remaining);
      newExpiry = remaining.length > 0 ? remaining[0].expiry : null;
    } else if (cleanBatchId === 'primary-' + targetItem.id) {
      removedBatch = { batchId: cleanBatchId, qty: targetItem.qty, expiry: targetItem.expiry };
      newQty = 0;
      newExpiry = null;
    } else {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Batch not found.' };
    }

    writeCellIfMapped_(info.sheet, targetItem._rowIndex, info.colMap.qty, newQty);
    writeExpiryCell_(info.sheet, targetItem._rowIndex, info.colMap.expiry, newExpiry);
    writeCellIfMapped_(info.sheet, targetItem._rowIndex, info.colMap.lastUpdated, now);
    SpreadsheetApp.flush();

    var catObj = parsed.categoryMap[targetItem.category] || DEFAULT_CATEGORIES[0];
    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        formatInventoryRow_(info.sheet, info.colMap, targetItem._rowIndex, catObj, newExpiry, tz);
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    appendHistoryLog_(ss, {
      timestamp: now,
      action: 'Discarded Batch',
      categoryName: catObj.name,
      itemName: targetItem.name,
      changeText: removedBatch.qty > 0 ? '-' + removedBatch.qty : '0',
      newStock: newQty,
      unit: targetItem.unit,
      expiry: removedBatch.expiry,
      notes: prefixActorNotes_('Discarded batch ' + cleanBatchId, actorLabel)
    });

    var unitSuffix = targetItem.unit ? ' ' + targetItem.unit : '';
    var freshData = readPantryDataInternal_(ss, info, pinToken);
    return {
      ok: true,
      message: 'Discarded batch of "' + targetItem.name + '" (-' + removedBatch.qty + unitSuffix + ', now ' + newQty + unitSuffix + ').',
      data: freshData
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Batch-saves multiple parsed or voice/quick-add pantry entries in a single lock.
 * entries = [{ action: 'add'|'use', matchedItemId, name, category, qty, unit, target, expiry, location, notes }]
 */
function saveBatchQuickEntries(entries, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!Array.isArray(entries) || entries.length === 0) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] No items provided to save.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var settings = readSettingsInternal_(ss);
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info, settings.categories);
    var batchIndex = readBatchesInternal_(ss);
    var now = new Date();

    var savedCount = 0;
    var addedNewCount = 0;
    var logEntries = [];
    var summaries = [];
    var styledRows = [];

    for (var i = 0; i < entries.length; i++) {
      var raw = entries[i];
      if (!raw || typeof raw !== 'object') continue;

      var action = String(raw.action || 'add').trim().toLowerCase() === 'use' ? 'use' : 'add';
      var qtyDelta = parseQuantity_(raw.qty !== undefined ? raw.qty : raw.qtyDelta);
      if (qtyDelta <= 0) continue;

      var cleanName = String(raw.name || '').trim();
      var existing = null;
      if (raw.matchedItemId) {
        existing = findItemById_(parsed.items, String(raw.matchedItemId).trim());
      }
      if (!existing && cleanName) {
        existing = findBestMatchingItem_(parsed.items, cleanName);
      }

      var parsedExp = raw.expiry !== undefined && raw.expiry !== null && raw.expiry !== ''
        ? parseExpiryValue_(raw.expiry, tz)
        : null;

      if (existing) {
        var catObj = parsed.categoryMap[existing.category] || settings.categories[0] || DEFAULT_CATEGORIES[0];
        var oldQty = existing.qty;
        var newQty = oldQty;
        var newExpiry = existing.expiry;
        var tracked = getBatchesForItem_(batchIndex, existing);

        if (action === 'add') {
          var incomingExp = parsedExp !== null ? parsedExp : existing.expiry;
          var shouldUseBatches = tracked.length > 0 || (oldQty > 0 && expiryKey_(incomingExp) !== expiryKey_(existing.expiry));

          if (shouldUseBatches) {
            var baseBatches = tracked.length > 0
              ? tracked
              : (oldQty > 0 ? [{ batchId: makeBatchId_(), qty: oldQty, expiry: existing.expiry, addedAt: formatSyncTimestamp_(now, tz) }] : []);
            var mergedOut = addOrMergeBatch_(baseBatches, qtyDelta, incomingExp, now);
            replaceItemBatches_(ss, existing.id, existing.name, mergedOut.batches);
            batchIndex = readBatchesInternal_(ss);
            newQty = mergedOut.totalQty;
            newExpiry = mergedOut.soonestExpiry;
          } else {
            newQty = roundQty_(oldQty + qtyDelta);
            newExpiry = incomingExp;
          }
        } else {
          if (tracked.length > 0) {
            var fifoRes = deductBatchesFifo_(tracked, qtyDelta);
            replaceItemBatches_(ss, existing.id, existing.name, fifoRes.remainingBatches);
            batchIndex = readBatchesInternal_(ss);
            newQty = fifoRes.totalQty;
            newExpiry = fifoRes.soonestExpiry;
          } else {
            newQty = Math.max(0, roundQty_(oldQty - qtyDelta));
          }
        }

        existing.qty = newQty;
        existing.expiry = newExpiry;

        writeCellIfMapped_(info.sheet, existing._rowIndex, info.colMap.qty, newQty);
        writeExpiryCell_(info.sheet, existing._rowIndex, info.colMap.expiry, newExpiry);
        writeCellIfMapped_(info.sheet, existing._rowIndex, info.colMap.lastUpdated, now);

        styledRows.push({ rowIndex: existing._rowIndex, catObj: catObj, expiry: newExpiry });
        var unitTxt = existing.unit ? ' ' + existing.unit : '';
        summaries.push((action === 'add' ? '+' : '-') + qtyDelta + unitTxt + ' ' + existing.name + ' (now ' + newQty + unitTxt + ')');
        logEntries.push({
          timestamp: now,
          action: action === 'add' ? 'Quick Batch Add' : 'Quick Batch Use',
          categoryName: catObj.name,
          itemName: existing.name,
          changeText: (action === 'add' ? '+' : '-') + qtyDelta,
          newStock: newQty,
          unit: existing.unit,
          expiry: newExpiry,
          notes: prefixActorNotes_(String(raw.notes || '').trim() || 'Saved via quick batch entry', actorLabel)
        });
        savedCount++;
      } else {
        if (!cleanName) continue;
        var resolvedCat = resolveCategory_(raw.category, parsed.categories, parsed.categoryMap) ||
          parsed.categoryMap[guessCategoryFromKeywords_(cleanName)] ||
          DEFAULT_CATEGORIES[0];
        var cleanUnit = String(raw.unit || '').trim() || 'packs';
        var parsedTarget = parseOptionalNonNegativeNumber_(raw.target);
        var finalTarget = parsedTarget !== undefined
          ? parsedTarget
          : Math.max(qtyDelta, defaultTargetForCategory_(resolvedCat.id));
        var cleanLocation = String(raw.location || '').trim() || (settings.locations[0] || 'Main Pantry');
        var cleanNotes = String(raw.notes || '').trim();
        var initialQty = action === 'use' ? 0 : qtyDelta;
        var newId = generateUniqueItemId_(cleanName, parsed.items);
        var nextRow = getNextAppendRow_(info);

        writeItemRowValues_(info.sheet, info.colMap, nextRow, {
          id: newId,
          categoryName: resolvedCat.name,
          name: cleanName,
          qty: initialQty,
          target: finalTarget,
          unit: cleanUnit,
          expiry: parsedExp,
          location: cleanLocation,
          notes: cleanNotes,
          lastUpdated: now
        });

        if (parsedExp !== null && initialQty > 0) {
          replaceItemBatches_(ss, newId, cleanName, [{
            batchId: makeBatchId_(),
            qty: initialQty,
            expiry: parsedExp,
            addedAt: formatSyncTimestamp_(now, tz)
          }]);
          batchIndex = readBatchesInternal_(ss);
        }

        var newItemObj = {
          id: newId,
          category: resolvedCat.id,
          name: cleanName,
          qty: initialQty,
          target: finalTarget,
          unit: cleanUnit,
          expiry: parsedExp,
          location: cleanLocation,
          notes: cleanNotes,
          _rowIndex: nextRow
        };
        parsed.items.push(newItemObj);
        styledRows.push({ rowIndex: nextRow, catObj: resolvedCat, expiry: parsedExp });
        summaries.push('Added ' + cleanName + ' (' + initialQty + ' ' + cleanUnit + ')');
        logEntries.push({
          timestamp: now,
          action: 'Added New',
          categoryName: resolvedCat.name,
          itemName: cleanName,
          changeText: '+' + initialQty,
          newStock: initialQty,
          unit: cleanUnit,
          expiry: parsedExp,
          notes: prefixActorNotes_(cleanNotes || 'Added via quick batch entry', actorLabel)
        });
        addedNewCount++;
        savedCount++;
      }
    }

    if (savedCount === 0) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] No valid items with positive quantity were found to save.' };
    }

    SpreadsheetApp.flush();

    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        for (var s = 0; s < styledRows.length; s++) {
          formatInventoryRow_(info.sheet, info.colMap, styledRows[s].rowIndex, styledRows[s].catObj, styledRows[s].expiry, tz);
        }
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    if (addedNewCount > 0) {
      organizeInventorySheet_(ss, info);
    }

    for (var l = 0; l < logEntries.length; l++) {
      appendHistoryLog_(ss, logEntries[l]);
    }

    var freshData = readPantryDataInternal_(ss, info, pinToken);
    return {
      ok: true,
      message: 'Saved ' + savedCount + ' item' + (savedCount === 1 ? '' : 's') + '!',
      savedCount: savedCount,
      summaries: summaries,
      data: freshData
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Looks up a barcode via Open Food Facts API v2 and returns clean product name,
 * brand, guessed category, and guessed unit.
 */
function lookupBarcode(code, pinToken) {
  if (!isValidPinToken_(pinToken)) {
    return buildLockedResponse_();
  }
  try {
    var cleanCode = String(code === null || code === undefined ? '' : code).replace(/\D/g, '');
    if (cleanCode.length < 6) {
      return { ok: false, error: 'Please scan or enter a valid barcode (at least 6 digits).' };
    }

    var url = 'https://world.openfoodfacts.org/api/v2/product/' + encodeURIComponent(cleanCode) + '.json?fields=product_name,brands,categories,quantity';
    var res = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
    var httpCode = typeof res.getResponseCode === 'function' ? res.getResponseCode() : 200;
    if (httpCode < 200 || httpCode >= 300) {
      return { ok: false, error: 'Barcode not found in Open Food Facts — you can type the name manually.' };
    }

    var body = JSON.parse(res.getContentText() || '{}');
    if (!body || body.status !== 1 || !body.product) {
      return { ok: false, error: 'Barcode not found in Open Food Facts — you can type the name manually.' };
    }

    var prod = body.product;
    var rawBrand = String(prod.brands || '').split(',')[0].trim();
    var rawName = String(prod.product_name || '').trim();
    if (!rawName && !rawBrand) {
      return { ok: false, error: 'Barcode found without a product name — you can type the name manually.' };
    }

    var finalName = rawName;
    if (rawBrand && rawName) {
      finalName = rawName.toLowerCase().indexOf(rawBrand.toLowerCase()) !== -1
        ? rawName
        : (rawBrand + ' ' + rawName);
    } else if (rawBrand) {
      finalName = rawBrand;
    }

    var category = guessCategoryFromKeywords_(finalName + ' ' + String(prod.categories || ''));
    var unit = guessUnitFromQuantityText_(String(prod.quantity || ''), finalName);

    return {
      ok: true,
      code: cleanCode,
      name: finalName,
      brand: rawBrand,
      category: category,
      unit: unit
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Barcode lookup failed: ' + getErrorMessage_(err) };
  }
}

/**
 * Reads up to `limit` (default 40) newest-first audit rows from the "History Log" tab.
 */
function getHistoryLog(limit, pinToken) {
  if (!isValidPinToken_(pinToken)) {
    return buildLockedResponse_();
  }
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var sheet = ss.getSheetByName(HISTORY_SHEET_NAME);
    if (!sheet || sheet.getLastRow() <= 1) {
      return { ok: true, entries: [] };
    }

    var maxLimit = Math.min(200, Math.max(1, Math.round(Number(limit) || 40)));
    var lastRow = sheet.getLastRow();
    var totalDataRows = lastRow - 1;
    var count = Math.min(maxLimit, totalDataRows);
    var startRow = lastRow - count + 1;
    var numCols = Math.min(HISTORY_HEADERS.length, Math.max(sheet.getLastColumn(), 1));
    var values = sheet.getRange(startRow, 1, count, numCols).getValues();

    var entries = [];
    for (var i = values.length - 1; i >= 0; i--) {
      var r = values[i];
      var rawTs = r[0];
      var tsText = rawTs instanceof Date
        ? formatSyncTimestamp_(rawTs, tz)
        : String(rawTs === null || rawTs === undefined ? '' : rawTs).trim();
      var changeStr = String(r[4] === null || r[4] === undefined ? '' : r[4]).replace(/^'/, '');

      entries.push({
        timestamp: tsText,
        action: String(r[1] || '').trim(),
        category: String(r[2] || '').trim(),
        itemName: String(r[3] || '').trim(),
        change: changeStr,
        newStock: r[5] !== undefined && r[5] !== '' ? r[5] : '',
        unit: String(r[6] || '').trim(),
        expiry: parseExpiryValue_(r[7], tz),
        notes: String(r[8] || '').trim()
      });
    }

    return { ok: true, entries: entries };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  }
}

/**
 * Detects the latest Telegram group or private chat ID for a bot token.
 */
function detectTelegramChatId(botToken, pinToken) {
  if (!isValidPinToken_(pinToken)) {
    return buildLockedResponse_();
  }
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var token = String(botToken || '').trim() || getTelegramBotToken_(ss);
    if (!token || token.indexOf(':') === -1) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Please enter a valid Telegram Bot Token from @BotFather.' };
    }

    // Temporarily delete any existing webhook so getUpdates works cleanly
    UrlFetchApp.fetch('https://api.telegram.org/bot' + encodeURIComponent(token) + '/deleteWebhook', {
      method: 'post',
      muteHttpExceptions: true
    });

    var res = UrlFetchApp.fetch('https://api.telegram.org/bot' + encodeURIComponent(token) + '/getUpdates?limit=20', {
      muteHttpExceptions: true
    });
    var data = JSON.parse(res.getContentText() || '{}');
    if (!data || !data.ok) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Telegram API rejected this token: ' + (data && data.description ? data.description : 'Check your Bot Token.') };
    }

    var updates = Array.isArray(data.result) ? data.result : [];
    var groupChat = null;
    var privateChat = null;

    for (var i = updates.length - 1; i >= 0; i--) {
      var u = updates[i];
      var chat = (u.message && u.message.chat) ||
        (u.my_chat_member && u.my_chat_member.chat) ||
        (u.callback_query && u.callback_query.message && u.callback_query.message.chat) ||
        (u.channel_post && u.channel_post.chat) ||
        null;
      if (!chat || chat.id === undefined || chat.id === null) continue;

      if ((chat.type === 'group' || chat.type === 'supergroup') && !groupChat) {
        groupChat = chat;
      } else if (!privateChat) {
        privateChat = chat;
      }
    }

    var chosen = groupChat || privateChat;
    if (!chosen) {
      return {
        ok: false,
        error: "Send any message (like 'hi') to your bot or Family Telegram group first, then tap Detect again!"
      };
    }

    return {
      ok: true,
      chatId: String(chosen.id),
      chatTitle: chosen.title || chosen.first_name || chosen.username || 'Telegram Chat'
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  }
}

/**
 * Connects Telegram bot token + chat ID, saves settings, registers webhook to this Web App URL,
 * and sends a warm welcome message with inline keyboard buttons into the Telegram chat.
 */
function setupTelegramWebhook(botToken, chatId, pinToken) {
  if (!isValidPinToken_(pinToken)) {
    return buildLockedResponse_();
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var token = String(botToken || '').trim() || getTelegramBotToken_(ss);
    var cleanChatId = String(chatId === null || chatId === undefined ? '' : chatId).trim();
    if (!token || token.indexOf(':') === -1) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Please enter a valid Telegram Bot Token.' };
    }
    if (!cleanChatId) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Please detect or enter a Telegram Chat ID first.' };
    }

    var webhookSecret = Utilities.getUuid();
    var props = getScriptProps_();
    if (props) {
      var tgPropMap = {};
      tgPropMap[TG_PROP_BOT_TOKEN] = token;
      tgPropMap[TG_PROP_WEBHOOK_SECRET] = webhookSecret;
      setPropsAtomic_(props, tgPropMap);
    }

    var current = readSettingsInternal_(ss);
    var updatedSettings = normalizeSettings_({
      telegramEnabled: true,
      telegramBotToken: token,
      telegramChatId: cleanChatId
    }, current);
    writeSettingsInternal_(ss, updatedSettings);

    var appUrl = getWebAppUrlSafe_();
    var webhookNote = '';
    if (appUrl && appUrl.indexOf('https://') === 0) {
      var signedWebhookUrl = appUrl + (appUrl.indexOf('?') === -1 ? '?' : '&') + 'secret=' + encodeURIComponent(webhookSecret);
      var whRes = UrlFetchApp.fetch('https://api.telegram.org/bot' + encodeURIComponent(token) + '/setWebhook', {
        method: 'post',
        contentType: 'application/json',
        payload: JSON.stringify({ url: signedWebhookUrl }),
        muteHttpExceptions: true
      });
      var whJson = JSON.parse(whRes.getContentText() || '{}');
      if (!whJson.ok) {
        webhookNote = ' (Webhook note: ' + (whJson.description || 'Could not register webhook') + ')';
      }
    } else {
      webhookNote = ' (Deploy as a Web App first to activate 2-way webhook replies.)';
    }

    var welcomeText =
      '🏠 *Family Pantry Bot Connected!*\n\n' +
      'Message me naturally anytime to update your Google Sheet:\n' +
      '• `used 2 tuna`\n' +
      '• `bought 20 lbs basmati rice exp may 2028`\n' +
      '• `check basmati rice`\n' +
      '• `/alerts` · `/shop` · `/days`';

    sendTelegramMessage_(token, cleanChatId, welcomeText, buildTelegramHomeKeyboard_(appUrl));

    var info = findOrCreateInventorySheet_(ss, false);
    if (info.isNewSheet) {
      info = findOrCreateInventorySheet_(ss, true);
    }

    return {
      ok: true,
      message: 'Telegram bot connected! Check your Telegram chat.' + webhookNote,
      webhookUrl: appUrl,
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Telegram Webhook HTTP POST endpoint (2-way Family Group Bot).
 * Always returns HTTP 200 'ok' so Telegram never gets stuck retrying.
 */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput("ok");
    }

    var storedSecret = getTelegramWebhookSecret_();
    var incomingSecret = (e && e.parameter && e.parameter.secret !== undefined && e.parameter.secret !== null)
      ? String(e.parameter.secret)
      : "";
    if (!storedSecret || !constantTimeEq_(incomingSecret, storedSecret)) {
      return ContentService.createTextOutput("ok");
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var settings = readSettingsInternal_(ss);
    if (settings.telegramEnabled !== true) {
      return ContentService.createTextOutput("ok");
    }

    var token = String(settings.telegramBotToken || "").trim() || getTelegramBotToken_(ss);
    if (!token) {
      return ContentService.createTextOutput("ok");
    }

    var allowedChatId = String(settings.telegramChatId || "").trim();
    if (!allowedChatId) {
      return ContentService.createTextOutput("ok");
    }

    var update = JSON.parse(e.postData.contents);
    var appUrl = getWebAppUrlSafe_();

    // 1. Inline button callbacks
    if (update.callback_query) {
      var cb = update.callback_query;
      var cbChatId = cb.message && cb.message.chat ? String(cb.message.chat.id) : "";
      if (!cbChatId || !constantTimeEq_(cbChatId, allowedChatId)) {
        return ContentService.createTextOutput("ok");
      }

      answerTelegramCallback_(token, cb.id, "Updating...");
      var cbData = String(cb.data || "").trim();

      if (cbData === "groceries") {
        sendTelegramMessage_(token, cbChatId, buildTelegramGroceriesText_(ss), buildTelegramHomeKeyboard_(appUrl));
      } else if (cbData === "alerts") {
        var alertDigest = buildNotificationDigest_(ss, settings);
        sendTelegramMessage_(token, cbChatId, "📋 *" + alertDigest.title + "*\n\n" + alertDigest.pushBody, buildTelegramHomeKeyboard_(appUrl));
      } else if (cbData === "shop") {
        sendTelegramMessage_(token, cbChatId, buildTelegramShoppingListText_(ss, settings), buildTelegramHomeKeyboard_(appUrl));
      } else if (cbData === "readiness") {
        sendTelegramMessage_(token, cbChatId, buildTelegramReadinessText_(ss, settings), buildTelegramHomeKeyboard_(appUrl));
      } else if (cbData.indexOf("adj:") === 0) {
        var parts = cbData.split(":");
        if (parts.length >= 3) {
          var adjDelta = Number(parts[parts.length - 1]);
          if (adjDelta !== 1 && adjDelta !== -1) {
            return ContentService.createTextOutput("ok");
          }
          var adjItemId = parts.slice(1, parts.length - 1).join(":");
          var adjRes = quickAdjustItem(adjItemId, adjDelta, getInternalPinToken_());
          var replyTxt = adjRes && adjRes.ok
            ? "✅ " + adjRes.message
            : "⚠️ " + (adjRes && adjRes.error ? adjRes.error : "Could not update item.");
          sendTelegramMessage_(token, cbChatId, replyTxt, buildTelegramHomeKeyboard_(appUrl));
        }
      }
      return ContentService.createTextOutput("ok");
    }

    // 2. Text messages
    var msg = update.message || update.edited_message;
    if (!msg || !msg.chat || !msg.text) {
      return ContentService.createTextOutput("ok");
    }

    var msgChatId = String(msg.chat.id);
    if (!msgChatId || !constantTimeEq_(msgChatId, allowedChatId)) {
      return ContentService.createTextOutput("ok");
    }

    var rawText = String(msg.text || "").trim();
    if (!rawText) {
      return ContentService.createTextOutput("ok");
    }

    // Strip @BotUsername from group slash commands like "/shop@MyPantryBot"
    var normalizedCmd = rawText.replace(/^(\/[a-zA-Z0-9_]+)@[a-zA-Z0-9_]+/, "$1").trim();
    var lowerCmd = normalizedCmd.toLowerCase();

    if (lowerCmd === "/start" || lowerCmd === "/help" || lowerCmd === "help") {
      var helpText =
        "🏠 *Family Pantry & Groceries Bot*\n\n" +
        "🛒 *Weekly House Groceries:*\n" +
        "• `buy milk, 2 eggs, whole wheat bread`\n" +
        "• `need 10 lbs halal chicken, biryani masala`\n" +
        "• `/groceries` — view your shared grocery list by store\n\n" +
        "📦 *Pantry Inventory Updates:*\n" +
        "• `used 2 tuna`\n" +
        "• `bought 20 lbs basmati rice exp may 2028`\n" +
        "• `check basmati rice` or `how much tuna`\n\n" +
        "Quick commands:\n" +
        "• `/groceries` — Weekly Groceries by Store\n" +
        "• `/alerts` — Expired, Use Soon & Low Stock\n" +
        "• `/shop` — Emergency Restock Shopping List\n" +
        "• `/days` — Days of Food Readiness";
      sendTelegramMessage_(token, msgChatId, helpText, buildTelegramHomeKeyboard_(appUrl));
      return ContentService.createTextOutput("ok");
    }

    if (lowerCmd === "/groceries" || lowerCmd === "/grocery" || lowerCmd === "groceries" || lowerCmd === "grocery" || lowerCmd === "grocery list" || lowerCmd === "/list" || lowerCmd === "list") {
      sendTelegramMessage_(token, msgChatId, buildTelegramGroceriesText_(ss), buildTelegramHomeKeyboard_(appUrl));
      return ContentService.createTextOutput("ok");
    }

    if (lowerCmd === "/alerts" || lowerCmd === "alerts") {
      var dig = buildNotificationDigest_(ss, settings);
      sendTelegramMessage_(token, msgChatId, "📋 *" + dig.title + "*\n\n" + dig.pushBody, buildTelegramHomeKeyboard_(appUrl));
      return ContentService.createTextOutput("ok");
    }

    if (lowerCmd === "/shop" || lowerCmd === "shop" || lowerCmd === "shopping list" || lowerCmd === "/shopping") {
      sendTelegramMessage_(token, msgChatId, buildTelegramShoppingListText_(ss, settings), buildTelegramHomeKeyboard_(appUrl));
      return ContentService.createTextOutput("ok");
    }

    if (lowerCmd === "/days" || lowerCmd === "/status" || lowerCmd === "days of food") {
      sendTelegramMessage_(token, msgChatId, buildTelegramReadinessText_(ss, settings), buildTelegramHomeKeyboard_(appUrl));
      return ContentService.createTextOutput("ok");
    }

    // Weekly Groceries: /buy <items> or need <items> or buy <items>
    var buyMatch = normalizedCmd.match(/^(?:\/buy|\/need|\/add|buy|need|add\s+to\s+groceries|groceries\s+add)\s+(.+)$/i);
    if (buyMatch) {
      var rawList = buyMatch[1].trim();
      var rawItems = rawList.split(/[,;\n]+|\s+and\s+/i);
      var entriesToBatch = [];
      var senderName = (msg.from && (msg.from.first_name || msg.from.username)) ? msg.from.first_name : "Telegram";
      for (var r = 0; r < rawItems.length; r++) {
        var piece = rawItems[r].trim();
        if (!piece) continue;
        var parsed = parseQuickItemPiece_(piece);
        if (parsed && parsed.name) {
          entriesToBatch.push({
            name: parsed.name,
            qty: parsed.qty || 1,
            unit: parsed.unit || "",
            aisle: guessGroceryAisle_(parsed.name),
            store: guessGroceryStore_(parsed.name, ""),
            favorite: false,
            addedBy: "Telegram (" + senderName + ")"
          });
        }
      }

      if (entriesToBatch.length > 0) {
        var batchRes = addGroceryBatch(entriesToBatch, getInternalPinToken_());
        if (batchRes && batchRes.ok) {
          var lines = ["🛒 *Added to Weekly Groceries:*", ""];
          for (var b = 0; b < entriesToBatch.length; b++) {
            var ent = entriesToBatch[b];
            var qStr = (ent.qty > 1 || ent.unit) ? ent.qty + (ent.unit ? " " + ent.unit : "") + " " : "";
            var stIcon = /costco/i.test(ent.store) ? "🏬 " : (/halal/i.test(ent.store) ? "🥩 " : "🛒 ");
            lines.push("• " + qStr + "*" + ent.name + "* — " + stIcon + ent.store);
          }
          lines.push("");
          lines.push("_View and check off in your Family Pantry App!_");
          sendTelegramMessage_(token, msgChatId, lines.join("\n"), buildTelegramHomeKeyboard_(appUrl));
        } else {
          sendTelegramMessage_(
            token,
            msgChatId,
            "⚠️ Could not save to groceries: " + (batchRes && batchRes.error ? batchRes.error : "Unknown error"),
            buildTelegramHomeKeyboard_(appUrl)
          );
        }
        return ContentService.createTextOutput("ok");
      }
    }

    var checkMatch = normalizedCmd.match(/^(?:check|how\s+much|stock\s+of|find)\s+(.+)$/i);
    if (checkMatch) {
      var query = checkMatch[1].replace(/\?+$/, "").trim();
      sendTelegramItemCheck_(ss, token, msgChatId, query, appUrl);
      return ContentService.createTextOutput("ok");
    }

    // Natural-language pantry update!
    var info = findOrCreateInventorySheet_(ss, false);
    var currentData = info.isNewSheet
      ? { items: [], categories: settings.categories }
      : readPantryDataInternal_(ss, info);
    var parsedEntries = parseQuickPantryText_(normalizedCmd, currentData.items, currentData.categories, ss.getSpreadsheetTimeZone());

    if (parsedEntries.length === 0) {
      sendTelegramMessage_(
        token,
        msgChatId,
        "🤔 I did not spot a recognized command. Try something like:\n" +
        "• `buy milk, eggs, bread` (adds to weekly groceries)\n" +
        "• `used 2 tuna` (updates pantry)\n" +
        "• `bought 10 lbs basmati rice exp may 2028`\n" +
        "• `/groceries` (view weekly list)\n" +
        "• `check tuna`",
        buildTelegramHomeKeyboard_(appUrl)
      );
      return ContentService.createTextOutput("ok");
    }

    var batchSaveRes = saveBatchQuickEntries(parsedEntries, getInternalPinToken_());
    if (batchSaveRes && batchSaveRes.ok) {
      var lines = ["✅ *Pantry Updated!*", ""];
      var sumList = batchSaveRes.summaries || [];
      for (var s = 0; s < sumList.length; s++) {
        lines.push("• " + sumList[s]);
      }
      sendTelegramMessage_(token, msgChatId, lines.join("\n"), buildTelegramHomeKeyboard_(appUrl));
    } else {
      sendTelegramMessage_(
        token,
        msgChatId,
        "⚠️ Could not save update: " + (batchSaveRes && batchSaveRes.error ? batchSaveRes.error : "Unknown error"),
        buildTelegramHomeKeyboard_(appUrl)
      );
    }

    return ContentService.createTextOutput("ok");
  } catch (err) {
    Logger.log("doPost error: " + getErrorMessage_(err));
    return ContentService.createTextOutput("ok");
  }
}

function seedStarterFamilyList(pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var settings = readSettingsInternal_(ss);
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info, settings.categories);

    var existingIds = {};
    var existingNames = {};
    for (var i = 0; i < parsed.items.length; i++) {
      existingIds[parsed.items[i].id] = true;
      existingNames[normalizeKey_(parsed.items[i].name)] = true;
    }

    var targetYears = settings.targetYears || 2;
    var yearScale = targetYears / 3;
    var starterItems = buildStarterFamilyItems_(tz);
    var now = new Date();
    var addedRows = [];

    for (var s = 0; s < starterItems.length; s++) {
      var item = starterItems[s];
      if (existingIds[item.id] || existingNames[normalizeKey_(item.name)]) {
        continue;
      }

      var catObj = parsed.categoryMap[item.category] || DEFAULT_CATEGORIES[0];
      var rowIndex = getNextAppendRow_(info);
      var scaledTarget = Math.abs(targetYears - 3) < 0.001
        ? item.target
        : Math.max(1, Math.round(item.target * yearScale));

      writeItemRowValues_(info.sheet, info.colMap, rowIndex, {
        id: item.id,
        categoryName: catObj.name,
        name: item.name,
        qty: item.qty,
        target: scaledTarget,
        unit: item.unit,
        expiry: item.expiry,
        location: item.location,
        notes: item.notes,
        lastUpdated: now
      });

      addedRows.push({ rowIndex: rowIndex, catObj: catObj, expiry: item.expiry });
    }

    SpreadsheetApp.flush();

    if (addedRows.length > 0 && info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        for (var a = 0; a < addedRows.length; a++) {
          formatInventoryRow_(info.sheet, info.colMap, addedRows[a].rowIndex, addedRows[a].catObj, addedRows[a].expiry, tz);
        }
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    var addedCount = addedRows.length;
    if (addedCount > 0) {
      organizeInventorySheet_(ss, info);

      appendHistoryLog_(ss, {
        timestamp: now,
        action: 'Starter List',
        categoryName: 'All Categories',
        itemName: addedCount + ' Family Starter Items',
        changeText: '+' + addedCount + ' items',
        newStock: parsed.items.length + addedCount,
        unit: 'items',
        expiry: null,
        notes: prefixActorNotes_('Seeded ' + targetYears + '-year starter list for ' + settings.adults + ' adults + ' + settings.kidAges.length + ' kids', actorLabel)
      });
    }

    var freshData = readPantryDataInternal_(ss, info, pinToken);
    var message;
    if (addedCount === starterItems.length) {
      message = 'Loaded 18 starter family items!';
    } else if (addedCount > 0) {
      message = 'Loaded ' + addedCount + ' new starter family items (skipped existing duplicates).';
    } else {
      message = 'All 18 starter family items are already in your pantry.';
    }

    return { ok: true, message: message, data: freshData };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Saves customizable app settings to the "Settings" tab, optionally scales
 * existing inventory targets when targetYears changes, and syncs notification triggers.
 */
function savePantrySettings(newSettings, options, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var currentSettings = readSettingsInternal_(ss);
    var normalized = normalizeSettings_(newSettings, currentSettings);

    var scaledCount = 0;
    var oldYears = options && options.scaleTargetsFromYears !== undefined
      ? Number(options.scaleTargetsFromYears)
      : NaN;

    var info = findOrCreateInventorySheet_(ss, false);
    if (
      isFinite(oldYears) &&
      oldYears > 0 &&
      Math.abs(oldYears - normalized.targetYears) > 0.001 &&
      !info.isNewSheet &&
      info.colMap.target
    ) {
      var ratio = normalized.targetYears / oldYears;
      var parsedForScale = readSheetInventory_(ss, info, normalized.categories);
      for (var i = 0; i < parsedForScale.items.length; i++) {
        var it = parsedForScale.items[i];
        if (typeof it.target === 'number' && isFinite(it.target) && it.target > 0) {
          var scaledTarget = Math.max(1, Math.round(it.target * ratio));
          info.sheet.getRange(it._rowIndex, info.colMap.target).setValue(scaledTarget);
          scaledCount++;
        }
      }
      if (scaledCount > 0) {
        SpreadsheetApp.flush();
        appendHistoryLog_(ss, {
          timestamp: new Date(),
          action: 'Scaled Goals',
          categoryName: 'All Categories',
          itemName: scaledCount + ' Item Goals',
          changeText: oldYears + 'y -> ' + normalized.targetYears + 'y',
          newStock: '',
          unit: 'items',
          expiry: null,
          notes: prefixActorNotes_('Scaled pantry stock goals from ' + oldYears + ' year(s) to ' + normalized.targetYears + ' year(s)', actorLabel)
        });
      }
    }

    writeSettingsInternal_(ss, normalized);

    var triggerNote = null;
    try {
      syncNotificationTrigger_(normalized);
    } catch (trigErr) {
      triggerNote = 'Settings saved, but automatic alert scheduling needs Apps Script trigger permission: ' + getErrorMessage_(trigErr);
    }

    if (info.isNewSheet) {
      info = findOrCreateInventorySheet_(ss, true);
    }

    var freshData = readPantryDataInternal_(ss, info, pinToken);
    var msg = scaledCount > 0
      ? 'Settings saved & scaled ' + scaledCount + ' item goals to ' + normalized.targetYears + ' year(s)!'
      : 'Settings saved!';

    return {
      ok: true,
      message: msg,
      triggerNote: triggerNote,
      data: freshData
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Batch-restocks checked items from the Auto Shopping List.
 * entries = [{ itemId: string, qtyToAdd: number }]
 */
function restockShoppingItems(entries, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!Array.isArray(entries) || entries.length === 0) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Please select at least one item to restock.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var settings = readSettingsInternal_(ss);
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info, settings.categories);

    var now = new Date();
    var restockedLogs = [];
    var styledRows = [];

    for (var i = 0; i < entries.length; i++) {
      var entry = entries[i];
      if (!entry || typeof entry !== 'object') continue;

      var qtyToAdd = parseQuantity_(entry.qtyToAdd);
      if (qtyToAdd <= 0) continue;

      var targetItem = null;
      if (entry.itemId) {
        targetItem = findItemById_(parsed.items, String(entry.itemId).trim());
      }
      if (!targetItem && entry.name) {
        targetItem = findItemByName_(parsed.items, normalizeKey_(entry.name), null);
      }
      if (!targetItem) continue;

      var newQty = roundQty_(targetItem.qty + qtyToAdd);
      targetItem.qty = newQty;

      writeCellIfMapped_(info.sheet, targetItem._rowIndex, info.colMap.qty, newQty);
      writeCellIfMapped_(info.sheet, targetItem._rowIndex, info.colMap.lastUpdated, now);

      var catObj = parsed.categoryMap[targetItem.category] || settings.categories[0] || DEFAULT_CATEGORIES[0];
      styledRows.push({ rowIndex: targetItem._rowIndex, catObj: catObj, expiry: targetItem.expiry });
      restockedLogs.push({
        timestamp: now,
        action: 'Shopping Restock',
        categoryName: catObj.name,
        itemName: targetItem.name,
        changeText: '+' + qtyToAdd,
        newStock: newQty,
        unit: targetItem.unit,
        expiry: targetItem.expiry,
        notes: prefixActorNotes_('Batch restocked from Auto Shopping List', actorLabel)
      });
    }

    if (restockedLogs.length === 0) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] No matching items with a positive restock quantity were found.' };
    }

    SpreadsheetApp.flush();

    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        for (var r = 0; r < styledRows.length; r++) {
          formatInventoryRow_(info.sheet, info.colMap, styledRows[r].rowIndex, styledRows[r].catObj, styledRows[r].expiry, tz);
        }
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    for (var l = 0; l < restockedLogs.length; l++) {
      appendHistoryLog_(ss, restockedLogs[l]);
    }

    var count = restockedLogs.length;
    var freshData = readPantryDataInternal_(ss, info, pinToken);
    return {
      ok: true,
      message: 'Restocked ' + count + ' item' + (count === 1 ? '' : 's') + '!',
      data: freshData
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/* ==========================================================================
   DAILY / WEEKLY HOUSE GROCERIES RPC ENDPOINTS ('Groceries' TAB)
   ========================================================================== */

/**
 * Adds a daily/weekly grocery item to the 'Groceries' tab.
 * If an unchecked item with the same normalized name already exists,
 * increments its quantity cleanly instead of creating a duplicate row.
 * entry = { name, qty, unit, aisle, favorite }
 */
function addGroceryItem(entry, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!entry || typeof entry !== "object") {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Invalid grocery item payload." };
    }

    var cleanName = String(entry.name === null || entry.name === undefined ? "" : entry.name).trim();
    if (!cleanName) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Please enter a grocery item name." };
    }

    var parsedQty = parseQuantity_(entry.qty);
    var qty = parsedQty > 0 ? parsedQty : 1;
    var cleanUnit = String(entry.unit === null || entry.unit === undefined ? "" : entry.unit).trim();
    var cleanAisle = String(entry.aisle === null || entry.aisle === undefined ? "" : entry.aisle).trim() || guessGroceryAisle_(cleanName);
    var cleanStore = String(entry.store === null || entry.store === undefined ? "" : entry.store).trim() || guessGroceryStore_(cleanName, cleanAisle);
    var favorite = Boolean(entry.favorite);
    var addedBy = actorLabel || String(entry.addedBy === null || entry.addedBy === undefined ? "" : entry.addedBy).trim();
    var now = new Date();

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ensureGroceriesSheet_(ss);
    var colMap = getGroceriesHeaderMap_(sheet);
    var existing = readGroceriesWithRows_(ss);
    var targetKey = normalizeKey_(cleanName);
    var match = null;

    for (var i = 0; i < existing.length; i++) {
      if (!existing[i].checked && normalizeKey_(existing[i].name) === targetKey) {
        match = existing[i];
        break;
      }
    }

    var info = findOrCreateInventorySheet_(ss, false);

    if (match) {
      var newQty = roundQty_(match.qty + qty);
      var finalUnit = match.unit || cleanUnit;
      var finalAisle = match.aisle || cleanAisle;
      var finalStore = match.store || cleanStore;
      var finalFav = Boolean(match.favorite || favorite);
      var finalAddedBy = addedBy || match.addedBy;

      var numCols = Math.max(sheet.getLastColumn(), GROCERIES_HEADERS.length);
      var rowValues = new Array(numCols).fill("");
      rowValues[colMap.id - 1] = match.id;
      rowValues[colMap.name - 1] = match.name;
      rowValues[colMap.qty - 1] = newQty;
      rowValues[colMap.unit - 1] = finalUnit;
      rowValues[colMap.aisle - 1] = finalAisle;
      if (colMap.store > 0) rowValues[colMap.store - 1] = finalStore;
      rowValues[colMap.checked - 1] = false;
      rowValues[colMap.favorite - 1] = finalFav;
      rowValues[colMap.addedBy - 1] = finalAddedBy;
      rowValues[colMap.updatedAt - 1] = now;

      sheet.getRange(match._rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
      SpreadsheetApp.flush();

      var unitSuffix = finalUnit ? " " + finalUnit : "";
      return {
        ok: true,
        merged: true,
        id: match.id,
        message: 'Updated \"' + match.name + '\" to ' + newQty + unitSuffix + ".",
        data: readPantryDataInternal_(ss, info, pinToken)
      };
    }

    var newId = makeUniquePrefixedId_("groc-", existing);
    var nextRow = sheet.getLastRow() + 1;
    var numColsNew = Math.max(sheet.getLastColumn(), GROCERIES_HEADERS.length);
    var rowValuesNew = new Array(numColsNew).fill("");
    rowValuesNew[colMap.id - 1] = newId;
    rowValuesNew[colMap.name - 1] = cleanName;
    rowValuesNew[colMap.qty - 1] = qty;
    rowValuesNew[colMap.unit - 1] = cleanUnit;
    rowValuesNew[colMap.aisle - 1] = cleanAisle;
    if (colMap.store > 0) rowValuesNew[colMap.store - 1] = cleanStore;
    rowValuesNew[colMap.checked - 1] = false;
    rowValuesNew[colMap.favorite - 1] = favorite;
    rowValuesNew[colMap.addedBy - 1] = addedBy;
    rowValuesNew[colMap.updatedAt - 1] = now;

    sheet.getRange(nextRow, 1, 1, rowValuesNew.length).setValues([rowValuesNew]);
    SpreadsheetApp.flush();

    return {
      ok: true,
      merged: false,
      id: newId,
      message: 'Added \"' + cleanName + '\" to Groceries.',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Batch adds multiple grocery items in a single sheet transaction with 1 lock & 1 flush.
 * Perfect for 1-Tap Meal Bundles, Telegram /buy with multiple items, and Low Stock auto-queue.
 */
function addGroceryBatch(entries, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var actorLabel = getActorLabelFromAuth_(auth);
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    if (!Array.isArray(entries) || entries.length === 0) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] No grocery items provided in batch." };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ensureGroceriesSheet_(ss);
    var colMap = getGroceriesHeaderMap_(sheet);
    var existing = readGroceriesWithRows_(ss);
    var now = new Date();
    var info = findOrCreateInventorySheet_(ss, false);

    var addedCount = 0;
    var mergedCount = 0;
    var summaries = [];

    // Map existing unchecked items by normalized name for fast deduplication
    var uncheckedMap = {};
    for (var i = 0; i < existing.length; i++) {
      if (!existing[i].checked) {
        uncheckedMap[normalizeKey_(existing[i].name)] = existing[i];
      }
    }

    var rowsToAppend = [];
    var numCols = Math.max(sheet.getLastColumn(), GROCERIES_HEADERS.length);

    for (var e = 0; e < entries.length; e++) {
      var item = entries[e];
      if (!item || typeof item !== "object") continue;
      var cleanName = String(item.name === null || item.name === undefined ? "" : item.name).trim();
      if (!cleanName) continue;

      var parsedQty = parseQuantity_(item.qty);
      var qty = parsedQty > 0 ? parsedQty : 1;
      var cleanUnit = String(item.unit === null || item.unit === undefined ? "" : item.unit).trim();
      var cleanAisle = String(item.aisle === null || item.aisle === undefined ? "" : item.aisle).trim() || guessGroceryAisle_(cleanName);
      var cleanStore = String(item.store === null || item.store === undefined ? "" : item.store).trim() || guessGroceryStore_(cleanName, cleanAisle);
      var favorite = Boolean(item.favorite);
      var addedBy = actorLabel || String(item.addedBy === null || item.addedBy === undefined ? "" : item.addedBy).trim();

      var normKey = normalizeKey_(cleanName);
      var match = uncheckedMap[normKey];

      if (match) {
        var newQty = roundQty_(match.qty + qty);
        var finalUnit = match.unit || cleanUnit;
        var finalAisle = match.aisle || cleanAisle;
        var finalStore = match.store || cleanStore;
        var finalFav = Boolean(match.favorite || favorite);
        var finalAddedBy = addedBy || match.addedBy;

        var rowValues = new Array(numCols).fill("");
        rowValues[colMap.id - 1] = match.id;
        rowValues[colMap.name - 1] = match.name;
        rowValues[colMap.qty - 1] = newQty;
        rowValues[colMap.unit - 1] = finalUnit;
        rowValues[colMap.aisle - 1] = finalAisle;
        if (colMap.store > 0) rowValues[colMap.store - 1] = finalStore;
        rowValues[colMap.checked - 1] = false;
        rowValues[colMap.favorite - 1] = finalFav;
        rowValues[colMap.addedBy - 1] = finalAddedBy;
        rowValues[colMap.updatedAt - 1] = now;

        sheet.getRange(match._rowIndex, 1, 1, rowValues.length).setValues([rowValues]);
        match.qty = newQty;
        mergedCount++;
        summaries.push("Updated " + match.name + " (" + newQty + (finalUnit ? " " + finalUnit : "") + ")");
      } else {
        var allSeen = existing.concat(rowsToAppend.map(function (r) { return { id: r.id }; }));
        var newId = makeUniquePrefixedId_("groc-", allSeen);
        var rowValuesNew = new Array(numCols).fill("");
        rowValuesNew[colMap.id - 1] = newId;
        rowValuesNew[colMap.name - 1] = cleanName;
        rowValuesNew[colMap.qty - 1] = qty;
        rowValuesNew[colMap.unit - 1] = cleanUnit;
        rowValuesNew[colMap.aisle - 1] = cleanAisle;
        if (colMap.store > 0) rowValuesNew[colMap.store - 1] = cleanStore;
        rowValuesNew[colMap.checked - 1] = false;
        rowValuesNew[colMap.favorite - 1] = favorite;
        rowValuesNew[colMap.addedBy - 1] = addedBy;
        rowValuesNew[colMap.updatedAt - 1] = now;

        rowsToAppend.push({ id: newId, values: rowValuesNew, name: cleanName });
        uncheckedMap[normKey] = {
          id: newId,
          name: cleanName,
          qty: qty,
          unit: cleanUnit,
          aisle: cleanAisle,
          store: cleanStore,
          _rowIndex: sheet.getLastRow() + rowsToAppend.length
        };
        addedCount++;
        summaries.push("Added " + cleanName + " (" + qty + (cleanUnit ? " " + cleanUnit : "") + ")");
      }
    }

    if (rowsToAppend.length > 0) {
      var startRow = sheet.getLastRow() + 1;
      var matrix = rowsToAppend.map(function (r) { return r.values; });
      sheet.getRange(startRow, 1, matrix.length, matrix[0].length).setValues(matrix);
    }
    SpreadsheetApp.flush();

    var msg = (addedCount > 0 && mergedCount > 0)
      ? "Added " + addedCount + " and updated " + mergedCount + " grocery items."
      : (addedCount > 0
        ? "Added " + addedCount + " grocery item" + (addedCount === 1 ? "" : "s") + "."
        : "Updated " + mergedCount + " grocery item" + (mergedCount === 1 ? "" : "s") + ".");

    return {
      ok: true,
      addedCount: addedCount,
      mergedCount: mergedCount,
      summaries: summaries,
      message: msg,
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Finds all low-stock or expiring emergency pantry items and batch-adds them to Weekly Groceries.
 */
function autoQueueLowPantryToGroceries(pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, false);
    if (info.isNewSheet) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] No pantry inventory items found." };
    }
    var pantryData = readPantryDataInternal_(ss, info, pinToken);
    var items = pantryData.items || [];
    var existingGroceries = readGroceriesInternal_(ss);
    var existingUnchecked = {};
    for (var g = 0; g < existingGroceries.length; g++) {
      if (!existingGroceries[g].checked) {
        existingUnchecked[normalizeKey_(existingGroceries[g].name)] = true;
      }
    }

    var entriesToAdd = [];
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
      var isLow = Boolean(item.isLow || (item.minTarget > 0 && item.qty <= (item.minTarget * 0.4)));
      var isExp = Boolean(item.isExpired || item.isSoon);
      if (isLow || isExp) {
        var key = normalizeKey_(item.name);
        if (!existingUnchecked[key]) {
          var neededQty = 1;
          if (item.minTarget > item.qty) {
            neededQty = Math.max(1, Math.round(item.minTarget - item.qty));
          }
          entriesToAdd.push({
            name: item.name,
            qty: neededQty,
            unit: item.unit || "",
            aisle: guessGroceryAisle_(item.name),
            store: guessGroceryStore_(item.name, item.category),
            favorite: false,
            addedBy: "Pantry Low Alert"
          });
        }
      }
    }

    if (entriesToAdd.length === 0) {
      return {
        ok: true,
        queuedCount: 0,
        message: "All low or expiring pantry items are already on your grocery list!",
        data: pantryData
      };
    }

    return addGroceryBatch(entriesToAdd, pinToken);
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  }
}

/**
 * Checks or unchecks a grocery item in the "Groceries" tab.
 */
function toggleGroceryChecked(groceryId, checked, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var cleanId = String(groceryId === null || groceryId === undefined ? "" : groceryId).trim();
    if (!cleanId) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Missing grocery item ID." };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
    if (!sheet) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    var colMap = getGroceriesHeaderMap_(sheet);
    var rows = readGroceriesWithRows_(ss);
    var target = findGroceryById_(rows, cleanId);
    if (!target) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    var nextChecked = (checked === undefined || checked === null) ? !target.checked : Boolean(checked);
    sheet.getRange(target._rowIndex, colMap.checked).setValue(nextChecked);
    sheet.getRange(target._rowIndex, colMap.updatedAt).setValue(new Date());
    SpreadsheetApp.flush();

    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      id: target.id,
      checked: nextChecked,
      message: (nextChecked ? 'Checked off \"' : 'Unchecked \"') + target.name + '\".',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Stars or unstars a grocery item as a weekly favorite in the "Groceries" tab.
 */
function toggleGroceryFavorite(groceryId, favorite, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var cleanId = String(groceryId === null || groceryId === undefined ? "" : groceryId).trim();
    if (!cleanId) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Missing grocery item ID." };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
    if (!sheet) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    var colMap = getGroceriesHeaderMap_(sheet);
    var rows = readGroceriesWithRows_(ss);
    var target = findGroceryById_(rows, cleanId);
    if (!target) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    var nextFav = (favorite === undefined || favorite === null) ? !target.favorite : Boolean(favorite);
    sheet.getRange(target._rowIndex, colMap.favorite).setValue(nextFav);
    sheet.getRange(target._rowIndex, colMap.updatedAt).setValue(new Date());
    SpreadsheetApp.flush();

    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      id: target.id,
      favorite: nextFav,
      message: (nextFav ? 'Starred \"' : 'Unstarred \"') + target.name + '\".',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Deletes a single grocery item row from the "Groceries" tab.
 */
function deleteGroceryItem(groceryId, pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var cleanId = String(groceryId === null || groceryId === undefined ? "" : groceryId).trim();
    if (!cleanId) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Missing grocery item ID." };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
    if (!sheet) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    var rows = readGroceriesWithRows_(ss);
    var target = findGroceryById_(rows, cleanId);
    if (!target) {
      return { ok: false, error: "[" + BACKEND_VERSION + "] Grocery item not found." };
    }

    sheet.deleteRow(target._rowIndex);
    SpreadsheetApp.flush();

    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      id: target.id,
      message: 'Removed \"' + target.name + '\" from Groceries.',
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Clears all checked (In Cart) grocery items from the "Groceries" tab.
 */
function clearCheckedGroceries(pinToken) {
  var auth = resolvePinToken_(pinToken);
  if (!auth.valid) {
    return buildLockedResponse_();
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
    if (!sheet) {
      return { ok: true, clearedCount: 0, message: "Groceries list is empty.", data: readPantryDataInternal_(ss, findOrCreateInventorySheet_(ss, false), pinToken) };
    }

    var rows = readGroceriesWithRows_(ss);
    var checkedRows = [];
    for (var i = 0; i < rows.length; i++) {
      if (rows[i].checked) {
        checkedRows.push(rows[i]._rowIndex);
      }
    }

    if (checkedRows.length === 0) {
      return { ok: true, clearedCount: 0, message: "No checked grocery items to clear.", data: readPantryDataInternal_(ss, findOrCreateInventorySheet_(ss, false), pinToken) };
    }

    // Delete in reverse order to preserve 1-based row indices
    checkedRows.sort(function (a, b) { return b - a; });
    for (var d = 0; d < checkedRows.length; d++) {
      sheet.deleteRow(checkedRows[d]);
    }
    SpreadsheetApp.flush();

    var info = findOrCreateInventorySheet_(ss, false);
    return {
      ok: true,
      clearedCount: checkedRows.length,
      message: "Cleared " + checkedRows.length + " checked grocery item" + (checkedRows.length === 1 ? "" : "s") + ".",
      data: readPantryDataInternal_(ss, info, pinToken)
    };
  } catch (err) {
    return { ok: false, error: "[" + BACKEND_VERSION + "] " + getErrorMessage_(err) };
  } finally {
    releaseLockSafe_(lock);
  }
}

function sendTestNotification(overrideConfig, pinToken) {
  if (!isValidPinToken_(pinToken)) {
    return buildLockedResponse_();
  }
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var saved = readSettingsInternal_(ss);
    var merged = (overrideConfig && typeof overrideConfig === 'object')
      ? normalizeSettings_(overrideConfig, saved)
      : saved;

    var fiAddrs = parseGoogleFiAddresses_(merged.notifyFiPhones);
    var hasCalendar = merged.notifyCalendar === true;
    var hasGmail = merged.notifyEmailEnabled === true && Boolean(String(merged.notifyEmail || '').trim());
    var hasTelegram = merged.telegramEnabled === true &&
      Boolean(String(merged.telegramBotToken || '').trim()) &&
      Boolean(String(merged.telegramChatId || '').trim());

    if (fiAddrs.length === 0 && !hasCalendar && !hasGmail && !hasTelegram) {
      return {
        ok: false,
        error: '[' + BACKEND_VERSION + '] Turn on Google Calendar alerts, Telegram Bot, enter a Google Fi phone number, or enable Gmail alerts first.'
      };
    }

    var digest = buildNotificationDigest_(ss, merged);
    var sendResult = sendNotificationChannels_(merged, digest, true);

    if (sendResult.sentChannels.length === 0) {
      return {
        ok: false,
        error: '[' + BACKEND_VERSION + '] Could not send test alert: ' + (sendResult.errors.join('; ') || 'No channel succeeded.')
      };
    }

    var msg = 'Test alert sent via ' + sendResult.sentChannels.join(' & ') + '!';
    if (sendResult.errors.length > 0) {
      msg += ' (Partial issue: ' + sendResult.errors.join('; ') + ')';
    }
    return {
      ok: true,
      message: msg,
      sentChannels: sendResult.sentChannels,
      errors: sendResult.errors
    };
  } catch (err) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  }
}

/**
 * Scheduled time-based trigger handler: checks inventory and sends Google Fi SMS,
 * Google Calendar Pop-Up, and/or optional Gmail alerts whenever alerts exist.
 */
function checkAndSendPantryNotifications(e) {
  try {
    var authorizedTrigger = false;
    if (e && e.triggerUid !== undefined && e.triggerUid !== null && String(e.triggerUid).trim() !== '') {
      var incomingUid = String(e.triggerUid).trim();
      var triggers = ScriptApp.getProjectTriggers();
      for (var t = 0; t < triggers.length; t++) {
        if (
          typeof triggers[t].getUniqueId === 'function' &&
          constantTimeEq_(String(triggers[t].getUniqueId()), incomingUid) &&
          triggers[t].getHandlerFunction() === 'checkAndSendPantryNotifications'
        ) {
          authorizedTrigger = true;
          break;
        }
      }
    }
    var authorizedByPin = Boolean(
      e && typeof e.pinToken === 'string' && e.pinToken.length > 0 && isPinSet_() && isValidPinToken_(e.pinToken)
    );
    if (!authorizedTrigger && !authorizedByPin) {
      return { ok: false, error: 'Unauthorized trigger call.' };
    }

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var settings = readSettingsInternal_(ss);
    if (settings.notifyFrequency === 'off') {
      return { ok: true, skipped: true, reason: 'Notifications are turned off.' };
    }

    var fiAddrs = parseGoogleFiAddresses_(settings.notifyFiPhones);
    var hasCalendar = settings.notifyCalendar === true;
    var hasGmail = settings.notifyEmailEnabled === true && Boolean(String(settings.notifyEmail || '').trim());
    var hasTelegram = settings.telegramEnabled === true &&
      Boolean(String(settings.telegramBotToken || '').trim()) &&
      Boolean(String(settings.telegramChatId || '').trim());
    if (fiAddrs.length === 0 && !hasCalendar && !hasGmail && !hasTelegram) {
      return { ok: true, skipped: true, reason: 'No notification channel configured.' };
    }

    var digest = buildNotificationDigest_(ss, settings);
    if (digest.counts.totalAlerts === 0) {
      return { ok: true, skipped: true, reason: 'No alerts to send.' };
    }

    var sendResult = sendNotificationChannels_(settings, digest, false);
    return {
      ok: sendResult.sentChannels.length > 0,
      sentChannels: sendResult.sentChannels,
      errors: sendResult.errors
    };
  } catch (err) {
    Logger.log('checkAndSendPantryNotifications error: ' + getErrorMessage_(err));
    return { ok: false, error: '[' + BACKEND_VERSION + '] ' + getErrorMessage_(err) };
  }
}

/* ==========================================================================
   OPTIONAL SYNCED GOOGLE FORM & TRIGGER
   ========================================================================== */

/**
 * Creates a Google Form linked to this spreadsheet and installs an onFormSubmit
 * trigger so submissions automatically update the pantry inventory tab.
 */
function createSyncedGoogleForm() {
  requireSheetUiContext_('Create Synced Google Form');
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    var categoryNames = [];
    for (var i = 0; i < parsed.categories.length; i++) {
      categoryNames.push(parsed.categories[i].name);
    }

    var formTitle = '🏠 Family Pantry — Quick Restock & Use Form';
    var form = FormApp.create(formTitle);
    form.setDescription(
      'Log items added to or used from the 3-year Family Pantry tracker. ' +
      'Submissions automatically sync to "' + info.sheet.getName() + '" and "' + HISTORY_SHEET_NAME + '".'
    );
    form.setConfirmationMessage('Saved! Your Family Pantry Google Sheet has been updated automatically.');

    form.addListItem()
      .setTitle('Category')
      .setChoiceValues(categoryNames)
      .setRequired(true);

    form.addMultipleChoiceItem()
      .setTitle('Action')
      .setChoiceValues(['Add / Restock', 'Use / Take Out'])
      .setRequired(true);

    form.addTextItem()
      .setTitle('Item Name')
      .setHelpText('Use the exact item name to update an existing item, or type a new name to add it.')
      .setRequired(true);

    var qtyValidation = FormApp.createTextValidation()
      .setHelpText('Enter a positive number (e.g., 2, 10, 0.5).')
      .requireNumberGreaterThan(0)
      .build();

    form.addTextItem()
      .setTitle('Quantity')
      .setValidation(qtyValidation)
      .setRequired(true);

    form.addListItem()
      .setTitle('Unit')
      .setHelpText('Optional for existing items.')
      .setChoiceValues(STANDARD_UNITS)
      .setRequired(false);

    var goalValidation = FormApp.createTextValidation()
      .setHelpText('Enter a non-negative number for your 3-year target.')
      .requireNumberGreaterThanOrEqualTo(0)
      .build();

    form.addTextItem()
      .setTitle('3-Year Goal')
      .setHelpText('Optional. Leave blank to keep existing target.')
      .setValidation(goalValidation)
      .setRequired(false);

    form.addDateItem()
      .setTitle('Expiration Date')
      .setHelpText('Optional. Leave blank if no expiry or to keep existing date.')
      .setRequired(false);

    form.addListItem()
      .setTitle('Storage Spot')
      .setHelpText('Optional. Leave blank to keep existing location.')
      .setChoiceValues(STANDARD_LOCATIONS)
      .setRequired(false);

    form.addParagraphTextItem()
      .setTitle('Notes')
      .setHelpText('Optional brand, rotation reminder, or kid age note.')
      .setRequired(false);

    form.setDestination(FormApp.DestinationType.SPREADSHEET, ss.getId());
    ensureFormSubmitTrigger_(ss);

    var formUrl = form.getPublishedUrl();
    var editUrl = form.getEditUrl();

    tryShowAlert_(
      '📝 Synced Google Form Created!',
      'Your Google Form is now linked to this spreadsheet and auto-syncs on every submit.\n\n' +
      'Shareable Form Link (bookmark on phone):\n' + formUrl + '\n\n' +
      'Form Editor Link:\n' + editUrl
    );

    return {
      ok: true,
      formUrl: formUrl,
      editUrl: editUrl,
      message: 'Created synced Google Form and installed auto-sync trigger!'
    };
  } catch (err) {
    var errMsg = getErrorMessage_(err);
    tryShowAlert_('Create Google Form Error', errMsg);
    return { ok: false, error: errMsg };
  } finally {
    releaseLockSafe_(lock);
  }
}

/**
 * Installable spreadsheet trigger handler for synced Google Form submissions.
 */
function onFormSubmit(e) {
  if (!(e && e.range && typeof e.range.getSheet === 'function')) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Unauthorized form trigger call.' };
  }
  var lock = LockService.getDocumentLock();
  try {
    lock.waitLock(LOCK_TIMEOUT_MS);

    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var tz = ss.getSpreadsheetTimeZone();
    var named = e.namedValues || {};

    var categoryVal = getNamedFormValue_(named, ['Category']);
    var actionVal = getNamedFormValue_(named, ['Action']);
    var itemName = getNamedFormValue_(named, ['Item Name', 'Item', 'Food', 'Product']);
    var qtyStr = getNamedFormValue_(named, ['Quantity', 'Qty', 'Amount']);
    var unitVal = getNamedFormValue_(named, ['Unit', 'Units']);
    var targetStr = getNamedFormValue_(named, ['3-Year Goal', 'Target', 'Goal']);
    var expiryStr = getNamedFormValue_(named, ['Expiration Date', 'Expiry', 'Best By']);
    var locationVal = getNamedFormValue_(named, ['Storage Spot', 'Location', 'Storage']);
    var notesVal = getNamedFormValue_(named, ['Notes', 'Note', 'Comments']);

    if (!itemName) return;

    var qtyDelta = parseQuantity_(qtyStr);
    if (qtyDelta <= 0) return;

    var isUseAction = /use|take|remove|consume|out/i.test(actionVal);
    var info = findOrCreateInventorySheet_(ss, true);
    var parsed = readSheetInventory_(ss, info);

    var normalizedName = normalizeKey_(itemName);
    var resolvedCat = resolveCategory_(categoryVal, parsed.categories, parsed.categoryMap);
    var existingItem = findItemByName_(parsed.items, normalizedName, resolvedCat ? resolvedCat.id : null);

    var parsedExpiry = expiryStr ? parseExpiryValue_(expiryStr, tz) : undefined;
    var parsedTarget = targetStr !== '' ? parseOptionalNonNegativeNumber_(targetStr) : undefined;
    var hasMetaUpdates = Boolean(
      unitVal || parsedTarget !== undefined || parsedExpiry !== undefined || locationVal || notesVal
    );

    if (existingItem) {
      var updatePayload = {
        mode: isUseAction ? 'use' : 'add',
        itemId: existingItem.id,
        category: categoryVal || existingItem.category,
        name: existingItem.name,
        qtyDelta: qtyDelta,
        updateMeta: hasMetaUpdates
      };
      if (unitVal) updatePayload.unit = unitVal;
      if (parsedTarget !== undefined) updatePayload.target = parsedTarget;
      if (parsedExpiry !== undefined) updatePayload.expiry = parsedExpiry;
      if (locationVal) updatePayload.location = locationVal;
      if (notesVal) updatePayload.notes = notesVal;

      saveFormEntryInternal_(ss, updatePayload, isUseAction ? 'Form Use' : 'Form Restock');
    } else if (!isUseAction) {
      var newPayload = {
        mode: 'new',
        itemId: null,
        category: categoryVal || 'grains',
        name: itemName,
        qtyDelta: qtyDelta,
        unit: unitVal || 'packs',
        target: parsedTarget !== undefined ? parsedTarget : Math.max(qtyDelta, defaultTargetForCategory_(resolvedCat ? resolvedCat.id : 'grains')),
        expiry: parsedExpiry !== undefined ? parsedExpiry : null,
        location: locationVal || 'Main Pantry',
        notes: notesVal || '',
        updateMeta: true
      };
      saveFormEntryInternal_(ss, newPayload, 'Form Added New');
    } else {
      appendHistoryLog_(ss, {
        timestamp: new Date(),
        action: 'Form Use (Unmatched)',
        categoryName: resolvedCat ? resolvedCat.name : (categoryVal || 'Uncategorized'),
        itemName: itemName,
        changeText: '-' + qtyDelta,
        newStock: 0,
        unit: unitVal,
        expiry: parsedExpiry || null,
        notes: 'Submitted via Google Form, but item did not exist in inventory yet.'
      });
    }

    SpreadsheetApp.flush();
  } catch (err) {
    Logger.log('onFormSubmit error: ' + getErrorMessage_(err));
  } finally {
    releaseLockSafe_(lock);
  }
}

/* ==========================================================================
   INTERNAL CORE SAVE & READ LOGIC
   ========================================================================== */

/**
 * Internal implementation of saveFormEntry (caller holds DocumentLock).
 */
function saveFormEntryInternal_(ss, payload, actionOverride, actorLabel, currentToken) {
  if (!payload || typeof payload !== 'object') {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Invalid form submission payload.' };
  }

  var mode = String(payload.mode || '').trim().toLowerCase();
  if (mode !== 'add' && mode !== 'use' && mode !== 'new') {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Invalid mode "' + mode + '". Expected add, use, or new.' };
  }

  var tz = ss.getSpreadsheetTimeZone();
  var info = findOrCreateInventorySheet_(ss, true);
  var parsed = readSheetInventory_(ss, info);

  var qtyDelta = parseQuantity_(payload.qtyDelta);
  var now = new Date();

  if (mode === 'new') {
    var cleanName = String(payload.name || '').trim();
    if (!cleanName) {
      return { ok: false, error: '[' + BACKEND_VERSION + '] Please enter an item name.' };
    }

    var newCat = resolveCategory_(payload.category, parsed.categories, parsed.categoryMap) || DEFAULT_CATEGORIES[0];
    var cleanUnit = String(payload.unit || '').trim() || 'packs';
    var parsedTarget = parseOptionalNonNegativeNumber_(payload.target);
    var finalTarget = parsedTarget !== undefined
      ? parsedTarget
      : Math.max(qtyDelta, defaultTargetForCategory_(newCat.id));
    var finalExpiry = parseExpiryValue_(payload.expiry, tz);
    var cleanLocation = String(payload.location || '').trim();
    var cleanNotes = String(payload.notes || '').trim();
    var newId = generateUniqueItemId_(cleanName, parsed.items);

    var rowIndex = getNextAppendRow_(info);
    writeItemRowValues_(info.sheet, info.colMap, rowIndex, {
      id: newId,
      categoryName: newCat.name,
      name: cleanName,
      qty: qtyDelta,
      target: finalTarget,
      unit: cleanUnit,
      expiry: finalExpiry,
      location: cleanLocation,
      notes: cleanNotes,
      lastUpdated: now
    });
    SpreadsheetApp.flush();

    if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
      try {
        formatInventoryRow_(info.sheet, info.colMap, rowIndex, newCat, finalExpiry, tz);
        SpreadsheetApp.flush();
      } catch (styleErr) {
        // Ignore cosmetic styling errors on Table sheets
      }
    }

    organizeInventorySheet_(ss, info);

    appendHistoryLog_(ss, {
      timestamp: now,
      action: actionOverride || 'Added New',
      categoryName: newCat.name,
      itemName: cleanName,
      changeText: '+' + qtyDelta,
      newStock: qtyDelta,
      unit: cleanUnit,
      expiry: finalExpiry,
      notes: prefixActorNotes_(cleanNotes, actorLabel)
    });

    var newData = readPantryDataInternal_(ss, info, currentToken);
    return {
      ok: true,
      message: 'Added "' + cleanName + '" (' + qtyDelta + ' ' + cleanUnit + ').',
      data: newData
    };
  }

  // Existing item lookup for 'add' or 'use'
  var existing = null;
  if (payload.itemId) {
    existing = findItemById_(parsed.items, String(payload.itemId).trim());
  }
  if (!existing && payload.name) {
    var fallbackCat = resolveCategory_(payload.category, parsed.categories, parsed.categoryMap);
    existing = findItemByName_(parsed.items, normalizeKey_(payload.name), fallbackCat ? fallbackCat.id : null);
  }
  if (!existing) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Selected item was not found in the spreadsheet. Please refresh and try again.' };
  }

  var shouldUpdateMeta = mode === 'add' ? payload.updateMeta !== false : payload.updateMeta === true;
  if (qtyDelta === 0 && !shouldUpdateMeta) {
    return { ok: false, error: '[' + BACKEND_VERSION + '] Please enter a quantity greater than 0.' };
  }

  var oldQty = existing.qty;
  var newQty = mode === 'add'
    ? roundQty_(oldQty + qtyDelta)
    : Math.max(0, roundQty_(oldQty - qtyDelta));

  var finalCategory = parsed.categoryMap[existing.category] || DEFAULT_CATEGORIES[0];
  var finalName = existing.name;
  var finalUnit = existing.unit;
  var finalTargetExisting = existing.target;
  var finalExpiryExisting = existing.expiry;
  var finalLocationExisting = existing.location;
  var finalNotesExisting = existing.notes;

  var editRow = existing._rowIndex;
  var hasPayloadExpiry = Object.prototype.hasOwnProperty.call(payload, 'expiry') && payload.expiry !== undefined;
  var parsedIncomingExpiry = hasPayloadExpiry ? parseExpiryValue_(payload.expiry, tz) : existing.expiry;

  // Apply non-expiry metadata updates first (preserving finalName for Batches syncing)
  if (shouldUpdateMeta) {
    if (payload.category !== undefined && String(payload.category || '').trim() !== '') {
      var updatedCat = resolveCategory_(payload.category, parsed.categories, parsed.categoryMap);
      if (updatedCat) {
        finalCategory = updatedCat;
        writeCellIfMapped_(info.sheet, editRow, info.colMap.category, finalCategory.name);
      }
    }
    if (payload.name !== undefined && String(payload.name || '').trim() !== '') {
      finalName = String(payload.name).trim();
      writeCellIfMapped_(info.sheet, editRow, info.colMap.name, finalName);
    }
    if (payload.unit !== undefined && String(payload.unit || '').trim() !== '') {
      finalUnit = String(payload.unit).trim();
      writeCellIfMapped_(info.sheet, editRow, info.colMap.unit, finalUnit);
    }
    if (payload.target !== undefined && payload.target !== null && payload.target !== '') {
      var maybeTarget = parseOptionalNonNegativeNumber_(payload.target);
      if (maybeTarget !== undefined) {
        finalTargetExisting = maybeTarget;
        writeCellIfMapped_(info.sheet, editRow, info.colMap.target, finalTargetExisting);
      }
    }
    if (Object.prototype.hasOwnProperty.call(payload, 'location') && payload.location !== undefined) {
      finalLocationExisting = String(payload.location || '').trim();
      writeCellIfMapped_(info.sheet, editRow, info.colMap.location, finalLocationExisting);
    }
    if (Object.prototype.hasOwnProperty.call(payload, 'notes') && payload.notes !== undefined) {
      finalNotesExisting = String(payload.notes || '').trim();
      writeCellIfMapped_(info.sheet, editRow, info.colMap.notes, finalNotesExisting);
    }
  }

  // Multi-Batch FIFO Expiry Tracking
  var batchIndex = readBatchesInternal_(ss);
  var trackedBatches = getBatchesForItem_(batchIndex, existing);
  var handledExpiryViaBatches = false;

  if (mode === 'add' && qtyDelta > 0) {
    var addExpiry = shouldUpdateMeta && hasPayloadExpiry ? parsedIncomingExpiry : existing.expiry;
    var needsMultiBatch = trackedBatches.length > 0 ||
      (oldQty > 0 && expiryKey_(addExpiry) !== expiryKey_(existing.expiry));

    if (needsMultiBatch) {
      var baseBatches = trackedBatches.length > 0
        ? trackedBatches
        : (oldQty > 0 ? [{ batchId: makeBatchId_(), qty: oldQty, expiry: existing.expiry, addedAt: formatSyncTimestamp_(now, tz) }] : []);
      var mergedBatches = addOrMergeBatch_(baseBatches, qtyDelta, addExpiry, now);
      replaceItemBatches_(ss, existing.id, finalName, mergedBatches.batches);
      if (normalizeKey_(finalName) !== normalizeKey_(existing.name)) {
        replaceItemBatches_(ss, '__rename_cleanup__', existing.name, []);
      }
      newQty = mergedBatches.totalQty;
      finalExpiryExisting = mergedBatches.soonestExpiry;
      writeExpiryCell_(info.sheet, editRow, info.colMap.expiry, finalExpiryExisting);
      handledExpiryViaBatches = true;
    }
  } else if (mode === 'use' && qtyDelta > 0 && trackedBatches.length > 0) {
    var fifoResult = deductBatchesFifo_(trackedBatches, qtyDelta);
    replaceItemBatches_(ss, existing.id, finalName, fifoResult.remainingBatches);
    newQty = fifoResult.totalQty;
    finalExpiryExisting = fifoResult.soonestExpiry;
    writeExpiryCell_(info.sheet, editRow, info.colMap.expiry, finalExpiryExisting);
    handledExpiryViaBatches = true;
  }

  if (!handledExpiryViaBatches && shouldUpdateMeta && hasPayloadExpiry) {
    finalExpiryExisting = parsedIncomingExpiry;
    writeExpiryCell_(info.sheet, editRow, info.colMap.expiry, finalExpiryExisting);
  }

  writeCellIfMapped_(info.sheet, editRow, info.colMap.qty, newQty);
  writeCellIfMapped_(info.sheet, editRow, info.colMap.lastUpdated, now);

  SpreadsheetApp.flush();

  if (info.sheet.getName() === DEFAULT_INVENTORY_SHEET_NAME) {
    try {
      formatInventoryRow_(info.sheet, info.colMap, editRow, finalCategory, finalExpiryExisting, tz);
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Ignore cosmetic styling errors on Table sheets
    }
  }

  var signedChange = mode === 'add' ? '+' + qtyDelta : '-' + qtyDelta;
  var logAction = actionOverride || (mode === 'add' ? (qtyDelta > 0 ? 'Restocked' : 'Updated Details') : 'Used');

  appendHistoryLog_(ss, {
    timestamp: now,
    action: logAction,
    categoryName: finalCategory.name,
    itemName: finalName,
    changeText: signedChange,
    newStock: newQty,
    unit: finalUnit,
    expiry: finalExpiryExisting,
    notes: prefixActorNotes_(finalNotesExisting, actorLabel)
  });

  var updatedData = readPantryDataInternal_(ss, info, currentToken);
  var unitText = finalUnit ? ' ' + finalUnit : '';
  var summaryMsg;
  if (mode === 'add') {
    summaryMsg = qtyDelta > 0
      ? 'Restocked "' + finalName + '" (+' + qtyDelta + unitText + ', now ' + newQty + unitText + ').'
      : 'Updated details for "' + finalName + '".';
  } else {
    summaryMsg = 'Used ' + qtyDelta + unitText + ' of "' + finalName + '" (now ' + newQty + unitText + ').';
  }

  return { ok: true, message: summaryMsg, data: updatedData };
}

/**
 * Builds the exact JSON contract returned by getPantryData() and mutation responses.
 */
function readPantryDataInternal_(ss, info, currentToken) {
  var tz = ss.getSpreadsheetTimeZone();
  var settings = readSettingsInternal_(ss);
  var isSkippedPlaceholder = Boolean(info && info.isNewSheet && info.sheet && shouldSkipSheetName_(info.sheet.getName()));
  var parsed = isSkippedPlaceholder
    ? { categories: cloneCategoriesList_(settings.categories), categoryMap: {}, items: [] }
    : readSheetInventory_(ss, info, settings.categories);
  var batchIndex = readBatchesInternal_(ss);

  var publicItems = [];
  for (var i = 0; i < parsed.items.length; i++) {
    var it = parsed.items[i];
    var tracked = getBatchesForItem_(batchIndex, it);
    var itemBatches = [];
    if (tracked.length > 0) {
      for (var b = 0; b < tracked.length; b++) {
        itemBatches.push({
          batchId: tracked[b].batchId,
          qty: tracked[b].qty,
          expiry: tracked[b].expiry,
          addedAt: tracked[b].addedAt
        });
      }
    } else if (it.qty > 0) {
      itemBatches.push({
        batchId: 'primary-' + it.id,
        qty: it.qty,
        expiry: it.expiry,
        addedAt: ''
      });
    }

    publicItems.push({
      id: it.id,
      category: it.category,
      name: it.name,
      qty: it.qty,
      target: it.target,
      unit: it.unit,
      expiry: it.expiry,
      location: it.location,
      notes: it.notes,
      batches: itemBatches
    });
  }

  var daysOfFood = computeDaysOfFood_(publicItems, settings);
  var groceries = readGroceriesInternal_(ss);
  var security = buildSecuritySummary_(currentToken);

  return {
    ok: true,
    version: BACKEND_VERSION,
    locked: false,
    pinSet: security.pinSet,
    pinLength: PIN_LENGTH,
    pinPromptDismissed: isPinPromptDismissed_(),
    spreadsheetName: ss.getName(),
    sheetName: isSkippedPlaceholder ? DEFAULT_INVENTORY_SHEET_NAME : info.sheet.getName(),
    categories: parsed.categories,
    items: publicItems,
    groceries: groceries,
    settings: sanitizeSettingsForClient_(settings, ss),
    security: security,
    daysOfFood: daysOfFood,
    lastSync: formatSyncTimestamp_(new Date(), tz)
  };
}

/* ==========================================================================
   DAILY / WEEKLY GROCERIES TAB HELPERS (100% READ-ONLY READS)
   ========================================================================== */

function makeGroceryId_() {
  var shortId = (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.getUuid === 'function')
    ? Utilities.getUuid().slice(0, 8)
    : Math.random().toString(36).slice(2, 10);
  return 'groc-' + shortId;
}

function parseSheetBool_(val) {
  if (val === true) return true;
  if (val === false || val === null || val === undefined) return false;
  if (typeof val === 'number') return val === 1;
  return /^(true|yes|1|y|x|✓|✔)$/i.test(String(val).trim());
}

function guessGroceryAisle_(name) {
  var s = String(name || '').toLowerCase();
  if (/\b(milk|yogurt|cheese|butter|cream|eggs?|curd|paneer|ghee|kefir|sour cream)\b/.test(s)) return 'Dairy';
  if (/\b(apple|apples|banana|bananas|berry|berries|strawberry|blueberry|grape|orange|lemon|lime|mango|onion|tomato|potato|garlic|ginger|spinach|lettuce|cucumber|carrot|cilantro|coriander|mint|pepper|broccoli|avocado|produce|salad|fruit|veg|vegetable)\b/.test(s)) return 'Produce';
  if (/\b(bread|naan|roti|tortilla|bagel|bun|muffin|croissant|bakery|pita|cake|donut)\b/.test(s)) return 'Bakery';
  if (/\b(chicken|beef|lamb|goat|mutton|fish|salmon|shrimp|turkey|meat|keema|halal|deli|sausage)\b/.test(s)) return 'Meat & Deli';
  if (/\b(frozen|ice cream|nuggets|fries|peas|waffles|paratha|samosa)\b/.test(s)) return 'Frozen';
  if (/\b(soap|shampoo|detergent|tissue|paper towel|toilet paper|trash bag|foil|wrap|sponge|dish|bleach|wipes|diapers|toothpaste|cleaner)\b/.test(s)) return 'Household';
  if (/\b(juice|water|soda|tea|coffee|drink|sparkling|milkshake|chai)\b/.test(s)) return 'Beverages';
  if (/\b(rice|flour|atta|pasta|cereal|oats|oil|sugar|salt|spice|masala|sauce|ketchup|honey|jam|peanut butter|beans|lentils|daal|dal|snack|chips|crackers|biscuit|cookies|nuts)\b/.test(s)) return 'Pantry';
  return 'Other';
}

function guessGroceryStore_(name, aisle) {
  var text = (String(name || "") + " " + String(aisle || "")).toLowerCase();
  if (/(?:halal|lamb|goat|mutton|beef|biryani|daal|dal|lentil|naan|samosa|roti|masala|cumin|coriander|turmeric|cardamom|ghee|paneer|desi)/i.test(text)) {
    return "Halal Market";
  }
  if (/(?:costco|bulk|diaper|wipe|paper towel|toilet paper|trash bag|detergent|rice|flour|sugar|oats|cooking oil|water case|kirkland)/i.test(text)) {
    return "Costco";
  }
  if (/(?:target|baby food|formula|soap|shampoo|body wash|toothpaste|toothbrush|lotion|first aid|bandages|tylenol|motrin)/i.test(text)) {
    return "Target";
  }
  return "Grocery Store";
}

function getGroceriesHeaderMap_(sheet) {
  var lastCol = Math.max(1, sheet.getLastColumn());
  var headers = sheet.getRange(1, 1, 1, lastCol).getValues()[0];
  var map = { id: 1, name: 2, qty: 3, unit: 4, aisle: 5, store: 0, checked: 6, favorite: 7, addedBy: 8, updatedAt: 9 };
  for (var c = 0; c < headers.length; c++) {
    var h = String(headers[c] || "").trim().toLowerCase();
    if (h === "id") map.id = c + 1;
    else if (h === "name") map.name = c + 1;
    else if (h === "qty" || h === "quantity") map.qty = c + 1;
    else if (h === "unit") map.unit = c + 1;
    else if (h === "aisle") map.aisle = c + 1;
    else if (h === "store") map.store = c + 1;
    else if (h === "checked" || h === "in cart" || h === "cart") map.checked = c + 1;
    else if (h === "favorite" || h === "fav") map.favorite = c + 1;
    else if (h === "added by" || h === "actor") map.addedBy = c + 1;
    else if (h === "updated at" || h === "updated") map.updatedAt = c + 1;
  }
  return map;
}

function ensureGroceriesSheet_(ss) {
  var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(GROCERIES_SHEET_NAME);
  }
  if (sheet.getLastRow() === 0) {
    var headerRange = sheet.getRange(1, 1, 1, GROCERIES_HEADERS.length);
    headerRange.setValues([GROCERIES_HEADERS]);
    SpreadsheetApp.flush();

    try {
      headerRange
        .setBackground(HEADER_BG_COLOR)
        .setFontColor(HEADER_TEXT_COLOR)
        .setFontWeight("bold")
        .setFontSize(10)
        .setVerticalAlignment("middle");
      sheet.setRowHeight(1, 32);
      sheet.setFrozenRows(1);
      var widths = [125, 220, 80, 90, 130, 120, 85, 85, 130, 160];
      for (var i = 0; i < widths.length; i++) {
        sheet.setColumnWidth(i + 1, widths[i]);
      }
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Ignore cosmetic styling errors; header values are already committed
    }
  } else {
    // Dynamic backward compatibility: if existing Groceries tab lacks Store column, insert it cleanly
    var colMap = getGroceriesHeaderMap_(sheet);
    if (colMap.store === 0) {
      var insertAfter = colMap.aisle > 0 ? colMap.aisle : 5;
      sheet.insertColumnAfter(insertAfter);
      sheet.getRange(1, insertAfter + 1).setValue("Store");
      SpreadsheetApp.flush();
      try {
        sheet.setColumnWidth(insertAfter + 1, 120);
      } catch (colErr) {
        // Ignore column width failure
      }
    }
  }
  return sheet;
}

/**
 * Reads all grocery rows with their 1-based sheet row index (100% read-only; never creates tab).
 */
function readGroceriesWithRows_(ss) {
  var sheet = ss.getSheetByName(GROCERIES_SHEET_NAME);
  if (!sheet || sheet.getLastRow() <= 1 || sheet.getLastColumn() < 2) {
    return [];
  }

  var tz = ss.getSpreadsheetTimeZone();
  var numRows = sheet.getLastRow() - 1;
  var colMap = getGroceriesHeaderMap_(sheet);
  var numCols = Math.max(sheet.getLastColumn(), GROCERIES_HEADERS.length);
  var values = sheet.getRange(2, 1, numRows, numCols).getValues();
  var items = [];

  for (var i = 0; i < values.length; i++) {
    var r = values[i];
    var rowIndex = i + 2;
    var nameIdx = colMap.name - 1;
    var name = (nameIdx < r.length && r[nameIdx] !== null && r[nameIdx] !== undefined) ? String(r[nameIdx]).trim() : "";
    if (!name) continue;

    var idIdx = colMap.id - 1;
    var id = (idIdx < r.length && r[idIdx] !== null && r[idIdx] !== undefined) ? String(r[idIdx]).trim() : "";
    if (!id) id = "groc-r" + rowIndex;

    var qtyIdx = colMap.qty - 1;
    var parsedQty = qtyIdx < r.length ? parseQuantity_(r[qtyIdx]) : 1;
    var qty = parsedQty > 0 ? parsedQty : 1;

    var unitIdx = colMap.unit - 1;
    var unit = (unitIdx < r.length && r[unitIdx] !== null && r[unitIdx] !== undefined) ? String(r[unitIdx]).trim() : "";

    var aisleIdx = colMap.aisle - 1;
    var aisle = (aisleIdx < r.length && r[aisleIdx] !== null && r[aisleIdx] !== undefined) ? String(r[aisleIdx]).trim() : "";

    var storeIdx = colMap.store > 0 ? (colMap.store - 1) : -1;
    var store = (storeIdx >= 0 && storeIdx < r.length && r[storeIdx] !== null && r[storeIdx] !== undefined)
      ? String(r[storeIdx]).trim()
      : "";
    if (!store) {
      store = guessGroceryStore_(name, aisle);
    }

    var checkedIdx = colMap.checked - 1;
    var checked = checkedIdx < r.length ? parseSheetBool_(r[checkedIdx]) : false;

    var favIdx = colMap.favorite - 1;
    var favorite = favIdx < r.length ? parseSheetBool_(r[favIdx]) : false;

    var actorIdx = colMap.addedBy - 1;
    var addedBy = (actorIdx < r.length && r[actorIdx] !== null && r[actorIdx] !== undefined) ? String(r[actorIdx]).trim() : "";

    var upIdx = colMap.updatedAt - 1;
    var rawUpdated = upIdx < r.length ? r[upIdx] : "";
    var updatedAt = rawUpdated instanceof Date
      ? formatSyncTimestamp_(rawUpdated, tz)
      : String(rawUpdated === null || rawUpdated === undefined ? "" : rawUpdated).trim();

    items.push({
      id: id,
      name: name,
      qty: qty,
      unit: unit,
      aisle: aisle,
      store: store,
      checked: checked,
      favorite: favorite,
      addedBy: addedBy,
      updatedAt: updatedAt,
      _rowIndex: rowIndex
    });
  }

  return items;
}

/**
 * Public-safe read-only Groceries list for getPantryData() / mutations.
 */
function readGroceriesInternal_(ss) {
  var rows = readGroceriesWithRows_(ss);
  var out = [];
  for (var i = 0; i < rows.length; i++) {
    var g = rows[i];
    out.push({
      id: g.id,
      name: g.name,
      qty: g.qty,
      unit: g.unit,
      aisle: g.aisle,
      store: g.store,
      checked: g.checked,
      favorite: g.favorite,
      addedBy: g.addedBy,
      updatedAt: g.updatedAt
    });
  }
  return out;
}

function findGroceryById_(rows, groceryId) {
  for (var i = 0; i < rows.length; i++) {
    if (rows[i].id === groceryId) return rows[i];
  }
  return null;
}

/* ==========================================================================
   SETTINGS PERSISTENCE & GOOGLE-ONLY NOTIFICATIONS (FI SMS / CALENDAR / GMAIL)
   ========================================================================== */

/**
 * Returns a fresh copy of the default v4.0 application settings.
 */
function buildDefaultSettings_() {
  return {
    targetYears: 2,
    adults: 2,
    kidAges: [9, 5, 0],
    useSoonDays: 90,
    lowStockPct: 40,
    categories: cloneDefaultCategories_(),
    locations: STANDARD_LOCATIONS.slice(),
    notifyFiPhones: '',
    notifyCalendar: true,
    notifyEmailEnabled: false,
    notifyEmail: '',
    notifyFrequency: 'off',
    notifyDay: 'Sunday',
    notifyHour: 9,
    telegramEnabled: false,
    telegramBotToken: '',
    telegramChatId: ''
  };
}

/**
 * Validates and normalizes an incoming settings object against defaults/fallback.
 */
function normalizeSettings_(raw, fallback) {
  var base = fallback ? normalizeSettingsBaseCopy_(fallback) : buildDefaultSettings_();
  if (!raw || typeof raw !== 'object') {
    return base;
  }

  if (raw.targetYears !== undefined && raw.targetYears !== null && raw.targetYears !== '') {
    var ty = Number(raw.targetYears);
    if (isFinite(ty) && ty >= 0.25 && ty <= 10) {
      base.targetYears = Math.round(ty * 100) / 100;
    }
  }

  if (raw.adults !== undefined && raw.adults !== null && raw.adults !== '') {
    var ad = Math.round(Number(raw.adults));
    if (isFinite(ad)) {
      base.adults = Math.min(20, Math.max(1, ad));
    }
  }

  if (Array.isArray(raw.kidAges)) {
    var ages = [];
    for (var k = 0; k < raw.kidAges.length; k++) {
      var ageNum = Number(raw.kidAges[k]);
      if (isFinite(ageNum) && ageNum >= 0 && ageNum <= 25) {
        ages.push(Math.round(ageNum));
      }
    }
    base.kidAges = ages;
  }

  if (raw.useSoonDays !== undefined && raw.useSoonDays !== null && raw.useSoonDays !== '') {
    var usd = Math.round(Number(raw.useSoonDays));
    if (isFinite(usd)) {
      base.useSoonDays = Math.min(365, Math.max(7, usd));
    }
  }

  if (raw.lowStockPct !== undefined && raw.lowStockPct !== null && raw.lowStockPct !== '') {
    var lsp = Math.round(Number(raw.lowStockPct));
    if (isFinite(lsp)) {
      base.lowStockPct = Math.min(95, Math.max(5, lsp));
    }
  }

  if (Array.isArray(raw.categories) && raw.categories.length > 0) {
    var cleanCats = [];
    var seenCatIds = {};
    for (var c = 0; c < raw.categories.length; c++) {
      var cat = raw.categories[c];
      if (!cat || typeof cat !== 'object') continue;
      var catName = String(cat.name || '').trim();
      if (!catName) continue;
      var rawId = String(cat.id || '').trim();
      var catId = slugify_(rawId || catName) || ('cat-' + (c + 1));
      var suffix = 2;
      var uniqueId = catId;
      while (seenCatIds[uniqueId]) {
        uniqueId = catId + '-' + suffix;
        suffix++;
      }
      seenCatIds[uniqueId] = true;

      var paletteFallback = CUSTOM_CATEGORY_PALETTE[hashString_(uniqueId) % CUSTOM_CATEGORY_PALETTE.length];
      var color = /^#[0-9a-fA-F]{3,8}$/.test(String(cat.color || '').trim())
        ? String(cat.color).trim()
        : paletteFallback.color;
      var soft = /^#[0-9a-fA-F]{3,8}$/.test(String(cat.soft || '').trim())
        ? String(cat.soft).trim()
        : paletteFallback.soft;
      var icon = String(cat.icon || '').trim() || paletteFallback.icon;

      cleanCats.push({
        id: uniqueId,
        name: catName,
        color: color,
        soft: soft,
        icon: icon
      });
    }
    if (cleanCats.length > 0) {
      base.categories = cleanCats;
    }
  }

  if (Array.isArray(raw.locations) && raw.locations.length > 0) {
    var cleanLocs = [];
    var seenLocs = {};
    for (var l = 0; l < raw.locations.length; l++) {
      var loc = String(raw.locations[l] || '').trim();
      if (!loc) continue;
      var locKey = loc.toLowerCase();
      if (seenLocs[locKey]) continue;
      seenLocs[locKey] = true;
      cleanLocs.push(loc);
    }
    if (cleanLocs.length > 0) {
      base.locations = cleanLocs;
    }
  }

  if (raw.notifyFiPhones !== undefined) {
    base.notifyFiPhones = String(raw.notifyFiPhones || '').trim();
  }

  if (raw.notifyCalendar !== undefined) {
    base.notifyCalendar = Boolean(raw.notifyCalendar);
  }

  if (raw.notifyEmailEnabled !== undefined) {
    base.notifyEmailEnabled = Boolean(raw.notifyEmailEnabled);
  }

  if (raw.notifyEmail !== undefined) {
    base.notifyEmail = String(raw.notifyEmail || '').trim();
  }

  if (raw.notifyFrequency !== undefined) {
    var freq = String(raw.notifyFrequency || '').trim().toLowerCase();
    if (freq === 'off' || freq === 'daily' || freq === 'weekly') {
      base.notifyFrequency = freq;
    }
  }

  if (raw.notifyDay !== undefined) {
    var dayInput = String(raw.notifyDay || '').trim().toLowerCase();
    for (var d = 0; d < VALID_WEEKDAYS.length; d++) {
      if (VALID_WEEKDAYS[d].toLowerCase() === dayInput) {
        base.notifyDay = VALID_WEEKDAYS[d];
        break;
      }
    }
  }

  if (raw.notifyHour !== undefined && raw.notifyHour !== null && raw.notifyHour !== '') {
    var hr = Math.round(Number(raw.notifyHour));
    if (isFinite(hr)) {
      base.notifyHour = Math.min(23, Math.max(0, hr));
    }
  }

  if (raw.telegramEnabled !== undefined) {
    base.telegramEnabled = Boolean(raw.telegramEnabled);
  }

  if (raw.clearTelegramBotToken === true) {
    base.telegramBotToken = '';
    base._clearTelegramBotToken = true;
  } else if (raw.telegramBotToken !== undefined && String(raw.telegramBotToken || '').trim() !== '') {
    base.telegramBotToken = String(raw.telegramBotToken).trim();
  }

  if (raw.telegramChatId !== undefined) {
    base.telegramChatId = String(raw.telegramChatId === null ? '' : raw.telegramChatId).trim();
  }

  base.hasTelegramBotToken = Boolean(String(base.telegramBotToken || '').trim());

  return base;
}

function normalizeSettingsBaseCopy_(src) {
  var def = buildDefaultSettings_();
  if (!src || typeof src !== 'object') return def;
  return {
    targetYears: src.targetYears !== undefined ? src.targetYears : def.targetYears,
    adults: src.adults !== undefined ? src.adults : def.adults,
    kidAges: Array.isArray(src.kidAges) ? src.kidAges.slice() : def.kidAges,
    useSoonDays: src.useSoonDays !== undefined ? src.useSoonDays : def.useSoonDays,
    lowStockPct: src.lowStockPct !== undefined ? src.lowStockPct : def.lowStockPct,
    categories: Array.isArray(src.categories) && src.categories.length > 0
      ? cloneCategoriesList_(src.categories)
      : def.categories,
    locations: Array.isArray(src.locations) && src.locations.length > 0
      ? src.locations.slice()
      : def.locations,
    notifyFiPhones: src.notifyFiPhones !== undefined ? String(src.notifyFiPhones) : def.notifyFiPhones,
    notifyCalendar: src.notifyCalendar !== undefined ? Boolean(src.notifyCalendar) : def.notifyCalendar,
    notifyEmailEnabled: src.notifyEmailEnabled !== undefined ? Boolean(src.notifyEmailEnabled) : def.notifyEmailEnabled,
    notifyEmail: src.notifyEmail !== undefined ? String(src.notifyEmail) : def.notifyEmail,
    notifyFrequency: src.notifyFrequency !== undefined ? String(src.notifyFrequency) : def.notifyFrequency,
    notifyDay: src.notifyDay !== undefined ? String(src.notifyDay) : def.notifyDay,
    notifyHour: src.notifyHour !== undefined ? src.notifyHour : def.notifyHour,
    telegramEnabled: src.telegramEnabled !== undefined ? Boolean(src.telegramEnabled) : def.telegramEnabled,
    telegramBotToken: src.telegramBotToken !== undefined ? String(src.telegramBotToken) : def.telegramBotToken,
    telegramChatId: src.telegramChatId !== undefined ? String(src.telegramChatId) : def.telegramChatId,
    hasTelegramBotToken: Boolean(src.telegramBotToken || src.hasTelegramBotToken)
  };
}

/**
 * Reads persisted settings from the "Settings" tab (hydrating telegramBotToken from ScriptProperties).
 * 100% read-only: if the tab or key does not exist, returns DEFAULT_SETTINGS without creating anything.
 */
function readSettingsInternal_(ss) {
  var result = buildDefaultSettings_();
  try {
    var sheet = ss.getSheetByName(SETTINGS_SHEET_NAME);
    if (sheet && sheet.getLastRow() >= 1 && sheet.getLastColumn() >= 2) {
      var values = sheet.getRange(1, 1, sheet.getLastRow(), 2).getValues();
      for (var r = 0; r < values.length; r++) {
        if (String(values[r][0] || '').trim() === SETTINGS_KEY) {
          var jsonStr = String(values[r][1] || '').trim();
          if (jsonStr) {
            var parsed = JSON.parse(jsonStr);
            result = normalizeSettings_(parsed, buildDefaultSettings_());
          }
          break;
        }
      }
    }
  } catch (err) {
    Logger.log('readSettingsInternal_ fallback to defaults: ' + getErrorMessage_(err));
    result = buildDefaultSettings_();
  }
  var propToken = getTelegramBotToken_(ss);
  if (propToken) {
    result.telegramBotToken = propToken;
  }
  result.hasTelegramBotToken = Boolean(String(result.telegramBotToken || '').trim());
  return result;
}

/**
 * Writes settings JSON to the "Settings" tab (creating it cleanly if needed).
 * Always stores telegramBotToken in ScriptProperties and strips plaintext token from the sheet!
 */
function writeSettingsInternal_(ss, settingsObj) {
  if (settingsObj && settingsObj._clearTelegramBotToken === true) {
    saveTelegramBotToken_('');
  } else if (settingsObj && String(settingsObj.telegramBotToken || '').trim()) {
    saveTelegramBotToken_(settingsObj.telegramBotToken);
  }

  var sheetSafeSettings = normalizeSettingsBaseCopy_(settingsObj);
  delete sheetSafeSettings.telegramBotToken;
  delete sheetSafeSettings.hasTelegramBotToken;

  var sheet = ss.getSheetByName(SETTINGS_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(SETTINGS_SHEET_NAME);
  }

  if (sheet.getLastRow() === 0) {
    var headerRange = sheet.getRange(1, 1, 1, SETTINGS_HEADERS.length);
    headerRange.setValues([SETTINGS_HEADERS]);
    SpreadsheetApp.flush();

    try {
      headerRange
        .setBackground(HEADER_BG_COLOR)
        .setFontColor(HEADER_TEXT_COLOR)
        .setFontWeight('bold')
        .setFontSize(10)
        .setVerticalAlignment('middle');
      sheet.setRowHeight(1, 32);
      sheet.setFrozenRows(1);
      sheet.setColumnWidth(1, 180);
      sheet.setColumnWidth(2, 520);
      sheet.setColumnWidth(3, 165);
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Ignore cosmetic styling errors; header values are already committed
    }
  }

  var targetRow = -1;
  var lastRow = sheet.getLastRow();
  if (lastRow >= 1) {
    var keyVals = sheet.getRange(1, 1, lastRow, 1).getValues();
    for (var r = 0; r < keyVals.length; r++) {
      if (String(keyVals[r][0] || '').trim() === SETTINGS_KEY) {
        targetRow = r + 1;
        break;
      }
    }
  }
  if (targetRow === -1) {
    targetRow = lastRow + 1;
  }

  sheet.getRange(targetRow, 1, 1, 3).setValues([[
    SETTINGS_KEY,
    JSON.stringify(sheetSafeSettings),
    new Date()
  ]]);
  SpreadsheetApp.flush();
}

/**
 * Creates, updates, or removes the time-based trigger for checkAndSendPantryNotifications.
 */
function syncNotificationTrigger_(settings) {
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    if (triggers[i].getHandlerFunction() === 'checkAndSendPantryNotifications') {
      ScriptApp.deleteTrigger(triggers[i]);
    }
  }

  if (!settings || settings.notifyFrequency === 'off') {
    return;
  }

  var hour = Math.min(23, Math.max(0, Math.round(Number(settings.notifyHour) || 9)));

  if (settings.notifyFrequency === 'daily') {
    ScriptApp.newTrigger('checkAndSendPantryNotifications')
      .timeBased()
      .everyDays(1)
      .atHour(hour)
      .create();
    return;
  }

  if (settings.notifyFrequency === 'weekly') {
    var dayKey = String(settings.notifyDay || 'Sunday').trim().toUpperCase();
    var weekDayEnum = (ScriptApp.WeekDay && ScriptApp.WeekDay[dayKey])
      ? ScriptApp.WeekDay[dayKey]
      : ScriptApp.WeekDay.SUNDAY;

    ScriptApp.newTrigger('checkAndSendPantryNotifications')
      .timeBased()
      .onWeekDay(weekDayEnum)
      .atHour(hour)
      .create();
  }
}

/**
 * Builds crisp plain-text (Phone Push) and styled HTML (Email) pantry alert digests.
 */
function buildNotificationDigest_(ss, overrideSettings) {
  var tz = ss.getSpreadsheetTimeZone();
  var settings = overrideSettings || readSettingsInternal_(ss);
  var info = findOrCreateInventorySheet_(ss, false);
  var parsed = info.isNewSheet
    ? { items: [], categories: settings.categories, categoryMap: {} }
    : readSheetInventory_(ss, info, settings.categories);

  var expiredItems = [];
  var useSoonItems = [];
  var lowStockItems = [];

  for (var i = 0; i < parsed.items.length; i++) {
    var item = parsed.items[i];
    var diffDays = getDiffDaysFromToday_(item.expiry, tz);
    if (diffDays !== null && diffDays < 0) {
      expiredItems.push({ item: item, diffDays: diffDays });
    } else if (diffDays !== null && diffDays <= settings.useSoonDays) {
      useSoonItems.push({ item: item, diffDays: diffDays });
    }

    if (item.target > 0) {
      var pct = Math.round((item.qty / item.target) * 100);
      if (pct < settings.lowStockPct) {
        lowStockItems.push({ item: item, pct: pct, need: Math.max(0, roundQty_(item.target - item.qty)) });
      }
    }
  }

  expiredItems.sort(function (a, b) { return a.diffDays - b.diffDays; });
  useSoonItems.sort(function (a, b) { return a.diffDays - b.diffDays; });
  lowStockItems.sort(function (a, b) { return a.pct - b.pct; });

  var appUrl = '';
  try {
    if (typeof ScriptApp !== 'undefined' && ScriptApp && typeof ScriptApp.getService === 'function') {
      appUrl = ScriptApp.getService().getUrl() || '';
    }
  } catch (urlErr) {
    appUrl = '';
  }

  var totalAlerts = expiredItems.length + useSoonItems.length + lowStockItems.length;
  var hasUrgent = expiredItems.length > 0 || lowStockItems.length > 0;

  var summaryParts = [];
  if (expiredItems.length > 0) summaryParts.push(expiredItems.length + ' Expired');
  if (useSoonItems.length > 0) summaryParts.push(useSoonItems.length + ' Use Soon');
  if (lowStockItems.length > 0) summaryParts.push(lowStockItems.length + ' Low Stock');

  var title = summaryParts.length > 0
    ? 'Family Pantry Alert: ' + summaryParts.join(', ')
    : 'Family Pantry: All Stock Looks Great!';

  // Crisp plain-text body for Calendar descriptions & Gmail text fallback
  var pushLines = [];
  if (totalAlerts === 0) {
    pushLines.push('All ' + parsed.items.length + ' pantry items are fresh and above ' + settings.lowStockPct + '% of your ' + settings.targetYears + '-year goal.');
  } else {
    if (expiredItems.length > 0) {
      pushLines.push('EXPIRED (' + expiredItems.length + '):');
      for (var e = 0; e < Math.min(expiredItems.length, 6); e++) {
        var ex = expiredItems[e];
        pushLines.push('• ' + ex.item.name + ' (' + Math.abs(ex.diffDays) + 'd ago)');
      }
      if (expiredItems.length > 6) pushLines.push('• +' + (expiredItems.length - 6) + ' more expired');
    }
    if (useSoonItems.length > 0) {
      pushLines.push('USE SOON <= ' + settings.useSoonDays + 'd (' + useSoonItems.length + '):');
      for (var u = 0; u < Math.min(useSoonItems.length, 6); u++) {
        var us = useSoonItems[u];
        pushLines.push('• ' + us.item.name + ' (' + us.diffDays + 'd left)');
      }
      if (useSoonItems.length > 6) pushLines.push('• +' + (useSoonItems.length - 6) + ' more expiring soon');
    }
    if (lowStockItems.length > 0) {
      pushLines.push('LOW STOCK < ' + settings.lowStockPct + '% (' + lowStockItems.length + '):');
      for (var l = 0; l < Math.min(lowStockItems.length, 6); l++) {
        var lo = lowStockItems[l];
        var unitStr = lo.item.unit ? ' ' + lo.item.unit : '';
        pushLines.push('• ' + lo.item.name + ': ' + lo.item.qty + '/' + lo.item.target + unitStr + ' (' + lo.pct + '%)');
      }
      if (lowStockItems.length > 6) pushLines.push('• +' + (lowStockItems.length - 6) + ' more low stock');
    }
  }
  if (appUrl) {
    pushLines.push('');
    pushLines.push('Open Pantry App: ' + appUrl);
  }
  var pushBody = pushLines.join('\n');

  // Compact SMS body (under ~280 chars) for Google Fi SMS (@msg.fi.google.com)
  var smsBody;
  if (totalAlerts === 0) {
    smsBody = 'Family Pantry: All stocked up! No expired or low items.' + (appUrl ? ' ' + appUrl : '');
  } else {
    var topNames = [];
    var seenSmsNames = {};
    var priorityLists = [expiredItems, useSoonItems, lowStockItems];
    for (var pl = 0; pl < priorityLists.length && topNames.length < 3; pl++) {
      for (var pi = 0; pi < priorityLists[pl].length && topNames.length < 3; pi++) {
        var nm = priorityLists[pl][pi].item.name;
        if (!seenSmsNames[nm]) {
          seenSmsNames[nm] = true;
          topNames.push(nm);
        }
      }
    }
    var smsCore = 'Family Pantry: ' + summaryParts.join(', ') + '.';
    if (topNames.length > 0) {
      smsCore += ' Check: ' + topNames.join(', ') + '.';
    }
    if (smsCore.length > 180) {
      smsCore = smsCore.slice(0, 177) + '...';
    }
    smsBody = smsCore + (appUrl ? ' ' + appUrl : '');
  }

  // Clean styled HTML email body
  var htmlParts = [];
  htmlParts.push('<div style="font-family:-apple-system,BlinkMacSystemFont,\'Segoe UI\',Roboto,sans-serif;max-width:580px;margin:0 auto;color:#0f172a;background:#f8fafc;padding:20px;border-radius:14px;">');
  htmlParts.push('<div style="background:#15803d;color:#ffffff;padding:16px 20px;border-radius:12px;">');
  htmlParts.push('<div style="font-size:18px;font-weight:700;">🏠 ' + escapeHtml_(title) + '</div>');
  htmlParts.push('<div style="font-size:12px;opacity:0.9;margin-top:4px;">' + escapeHtml_(ss.getName()) + ' · ' + settings.targetYears + '-Year Plan · ' + parsed.items.length + ' items tracked</div>');
  htmlParts.push('</div>');

  if (totalAlerts === 0) {
    htmlParts.push('<div style="background:#dcfce7;color:#166534;padding:16px;border-radius:10px;margin-top:14px;font-size:14px;">Everything looks great! Zero expired items, zero items expiring within ' + settings.useSoonDays + ' days, and no items below ' + settings.lowStockPct + '% stock.</div>');
  } else {
    if (expiredItems.length > 0) {
      htmlParts.push('<h3 style="color:#b91c1c;margin:16px 0 8px;font-size:15px;">🚨 Expired (' + expiredItems.length + ')</h3><ul style="margin:0;padding-left:20px;font-size:14px;">');
      for (var eh = 0; eh < expiredItems.length; eh++) {
        htmlParts.push('<li style="margin:4px 0;"><strong>' + escapeHtml_(expiredItems[eh].item.name) + '</strong> — expired ' + Math.abs(expiredItems[eh].diffDays) + ' day(s) ago</li>');
      }
      htmlParts.push('</ul>');
    }
    if (useSoonItems.length > 0) {
      htmlParts.push('<h3 style="color:#b45309;margin:16px 0 8px;font-size:15px;">⏳ Use Soon (within ' + settings.useSoonDays + ' days · ' + useSoonItems.length + ')</h3><ul style="margin:0;padding-left:20px;font-size:14px;">');
      for (var uh = 0; uh < useSoonItems.length; uh++) {
        htmlParts.push('<li style="margin:4px 0;"><strong>' + escapeHtml_(useSoonItems[uh].item.name) + '</strong> — ' + useSoonItems[uh].diffDays + ' day(s) left</li>');
      }
      htmlParts.push('</ul>');
    }
    if (lowStockItems.length > 0) {
      htmlParts.push('<h3 style="color:#1d4ed8;margin:16px 0 8px;font-size:15px;">🛒 Low Stock (below ' + settings.lowStockPct + '% · ' + lowStockItems.length + ')</h3><ul style="margin:0;padding-left:20px;font-size:14px;">');
      for (var lh = 0; lh < lowStockItems.length; lh++) {
        var li = lowStockItems[lh];
        var uTxt = li.item.unit ? ' ' + escapeHtml_(li.item.unit) : '';
        htmlParts.push('<li style="margin:4px 0;"><strong>' + escapeHtml_(li.item.name) + '</strong> — ' + li.item.qty + ' / ' + li.item.target + uTxt + ' (' + li.pct + '%, need +' + li.need + uTxt + ')</li>');
      }
      htmlParts.push('</ul>');
    }
  }

  if (appUrl) {
    htmlParts.push('<div style="margin-top:18px;text-align:center;"><a href="' + escapeHtml_(appUrl) + '" style="display:inline-block;background:#15803d;color:#ffffff;text-decoration:none;padding:11px 20px;border-radius:10px;font-weight:700;font-size:14px;">Open Family Pantry App</a></div>');
  }
  htmlParts.push('</div>');

  return {
    title: title,
    smsBody: smsBody,
    pushBody: pushBody,
    htmlBody: htmlParts.join(''),
    hasUrgent: hasUrgent,
    counts: {
      expired: expiredItems.length,
      useSoon: useSoonItems.length,
      lowStock: lowStockItems.length,
      totalAlerts: totalAlerts
    }
  };
}

/**
 * Dispatches notification digest over 100% Google-only channels:
 * 1. Google Fi SMS (<10digits>@msg.fi.google.com)
 * 2. Google Calendar Pop-Up Reminder (with optional guest invites)
 * 3. Optional Gmail HTML Email Summary
 */
function sendNotificationChannels_(settings, digest, isTest) {
  var sentChannels = [];
  var errors = [];
  var cleanEmail = String(settings.notifyEmail || '').trim();

  // Channel 1: Google Fi SMS
  var fiAddrs = parseGoogleFiAddresses_(settings.notifyFiPhones);
  if (fiAddrs.length > 0) {
    var fiSent = 0;
    for (var f = 0; f < fiAddrs.length; f++) {
      try {
        MailApp.sendEmail({
          to: fiAddrs[f],
          subject: '',
          body: (isTest ? '[TEST] ' : '') + digest.smsBody
        });
        fiSent++;
      } catch (fiErr) {
        errors.push('Google Fi SMS (' + fiAddrs[f] + ') failed: ' + getErrorMessage_(fiErr));
      }
    }
    if (fiSent > 0) {
      sentChannels.push('Google Fi SMS (' + fiSent + ')');
    }
  }

  // Channel 2: Google Calendar Pop-Up Reminder
  if (settings.notifyCalendar === true) {
    try {
      var cal = CalendarApp.getDefaultCalendar();
      var now = new Date();
      var duplicateToday = false;

      if (!isTest) {
        var todaysEvents = cal.getEventsForDay(now);
        for (var e = 0; e < todaysEvents.length; e++) {
          var evTitle = String(todaysEvents[e].getTitle() || '');
          if (evTitle.indexOf('🏠 Family Pantry') === 0) {
            duplicateToday = true;
            break;
          }
        }
      }

      if (!duplicateToday) {
        var offsetMins = isTest ? 2 : 5;
        var startTime = new Date(now.getTime() + offsetMins * 60 * 1000);
        var endTime = new Date(startTime.getTime() + 15 * 60 * 1000);
        var eventTitle = isTest ? '🏠 [TEST] Family Pantry Alert' : '🏠 ' + digest.title;
        var calOpts = {
          description: digest.pushBody,
          sendInvites: false
        };
        if (cleanEmail) {
          calOpts.guests = cleanEmail;
        }
        var ev = cal.createEvent(eventTitle, startTime, endTime, calOpts);
        ev.removeAllReminders();
        ev.addPopupReminder(0);
        sentChannels.push('Google Calendar Pop-Up');
      }
    } catch (calErr) {
      errors.push('Google Calendar Pop-Up failed: ' + getErrorMessage_(calErr));
    }
  }

  // Channel 3: Optional Gmail Email Summary
  if (settings.notifyEmailEnabled === true && cleanEmail) {
    try {
      MailApp.sendEmail({
        to: cleanEmail,
        subject: (isTest ? '[TEST] ' : '') + digest.title,
        body: digest.pushBody,
        htmlBody: digest.htmlBody
      });
      sentChannels.push('Gmail Email');
    } catch (mailErr) {
      errors.push('Gmail Email failed: ' + getErrorMessage_(mailErr));
    }
  }

  // Channel 4: Optional Telegram Family Group Bot
  var tgToken = String(settings.telegramBotToken || '').trim();
  var tgChatId = String(settings.telegramChatId || '').trim();
  if (settings.telegramEnabled === true && tgToken && tgChatId) {
    try {
      var appUrl = getWebAppUrlSafe_();
      var tgText = (isTest ? '🔔 *[TEST]* ' : '🏠 *') + digest.title + (isTest ? '' : '*') + '\n\n' + digest.pushBody;
      var tgKb = buildTelegramAlertKeyboard_(appUrl);
      var tgOk = sendTelegramMessage_(tgToken, tgChatId, tgText, tgKb);
      if (tgOk) {
        sentChannels.push('Telegram');
      } else {
        errors.push('Telegram API did not confirm delivery');
      }
    } catch (tgErr) {
      errors.push('Telegram failed: ' + getErrorMessage_(tgErr));
    }
  }

  return {
    sentChannels: sentChannels,
    errors: errors
  };
}

/* ==========================================================================
   v4.0 HELPERS: MULTI-BATCH FIFO, DAYS OF FOOD, NL PARSER & TELEGRAM BOT
   ========================================================================== */

function makeBatchId_() {
  var shortId = (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.getUuid === 'function')
    ? Utilities.getUuid().slice(0, 8)
    : Math.random().toString(36).slice(2, 10);
  return 'batch-' + shortId;
}

function expiryKey_(exp) {
  if (!exp || typeof exp !== 'object') return '';
  var m = String((exp.m || 0) + 1);
  if (m.length < 2) m = '0' + m;
  var d = String(exp.d || 1);
  if (d.length < 2) d = '0' + d;
  return exp.y + '-' + m + '-' + d;
}

function compareExpiryAsc_(a, b) {
  if (!a && !b) return 0;
  if (!a) return 1;  // null (No Expiry) goes last
  if (!b) return -1;
  if (a.y !== b.y) return a.y - b.y;
  if (a.m !== b.m) return a.m - b.m;
  return a.d - b.d;
}

function sortBatchesFifo_(list) {
  list.sort(function (a, b) {
    var cmp = compareExpiryAsc_(a.expiry, b.expiry);
    if (cmp !== 0) return cmp;
    return String(a.addedAt || '').localeCompare(String(b.addedAt || ''));
  });
  return list;
}

function sumBatchesQty_(list) {
  var total = 0;
  for (var i = 0; i < list.length; i++) {
    total += parseQuantity_(list[i].qty);
  }
  return roundQty_(total);
}

/**
 * Reads all non-zero batches from the "Batches" tab (100% read-only; never creates tab).
 */
function readBatchesInternal_(ss) {
  var byItemId = {};
  var byItemName = {};
  var rows = [];

  var sheet = ss.getSheetByName(BATCHES_SHEET_NAME);
  if (!sheet || sheet.getLastRow() <= 1 || sheet.getLastColumn() < 4) {
    return { byItemId: byItemId, byItemName: byItemName, rows: rows };
  }

  var tz = ss.getSpreadsheetTimeZone();
  var numRows = sheet.getLastRow() - 1;
  var numCols = Math.min(BATCHES_HEADERS.length, sheet.getLastColumn());
  var values = sheet.getRange(2, 1, numRows, numCols).getValues();

  for (var i = 0; i < values.length; i++) {
    var r = values[i];
    var qty = parseQuantity_(r[3]);
    if (qty <= 0) continue;

    var batchId = String(r[0] || '').trim() || ('batch-r' + (i + 2));
    var itemId = String(r[1] || '').trim();
    var itemName = String(r[2] || '').trim();
    if (!itemId && !itemName) continue;

    var expiry = numCols >= 5 ? parseExpiryValue_(r[4], tz) : null;
    var rawAdded = numCols >= 6 ? r[5] : '';
    var addedAt = rawAdded instanceof Date
      ? formatSyncTimestamp_(rawAdded, tz)
      : String(rawAdded === null || rawAdded === undefined ? '' : rawAdded).trim();

    var rec = {
      batchId: batchId,
      itemId: itemId,
      itemName: itemName,
      qty: qty,
      expiry: expiry,
      addedAt: addedAt
    };
    rows.push(rec);

    if (itemId) {
      if (!byItemId[itemId]) byItemId[itemId] = [];
      byItemId[itemId].push(rec);
    }
    var nKey = normalizeKey_(itemName);
    if (nKey) {
      if (!byItemName[nKey]) byItemName[nKey] = [];
      byItemName[nKey].push(rec);
    }
  }

  return { byItemId: byItemId, byItemName: byItemName, rows: rows };
}

function getBatchesForItem_(batchIndex, item) {
  if (!batchIndex || !item) return [];
  var found = [];
  var seen = {};

  function collect(arr) {
    if (!Array.isArray(arr)) return;
    for (var i = 0; i < arr.length; i++) {
      var b = arr[i];
      if (!seen[b.batchId]) {
        seen[b.batchId] = true;
        found.push({
          batchId: b.batchId,
          itemId: b.itemId,
          itemName: b.itemName,
          qty: b.qty,
          expiry: b.expiry,
          addedAt: b.addedAt
        });
      }
    }
  }

  collect(batchIndex.byItemId[item.id]);
  collect(batchIndex.byItemName[normalizeKey_(item.name)]);
  return sortBatchesFifo_(found);
}

function ensureBatchesSheet_(ss) {
  var sheet = ss.getSheetByName(BATCHES_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(BATCHES_SHEET_NAME);
  }
  if (sheet.getLastRow() === 0) {
    var headerRange = sheet.getRange(1, 1, 1, BATCHES_HEADERS.length);
    headerRange.setValues([BATCHES_HEADERS]);
    SpreadsheetApp.flush();

    try {
      headerRange
        .setBackground(HEADER_BG_COLOR)
        .setFontColor(HEADER_TEXT_COLOR)
        .setFontWeight('bold')
        .setFontSize(10)
        .setVerticalAlignment('middle');
      sheet.setRowHeight(1, 32);
      sheet.setFrozenRows(1);
      var widths = [130, 130, 220, 90, 135, 160];
      for (var i = 0; i < widths.length; i++) {
        sheet.setColumnWidth(i + 1, widths[i]);
      }
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Ignore cosmetic styling errors
    }
  }
  return sheet;
}

/**
 * Replaces all rows for (itemId / itemName) in the "Batches" tab with newBatches.
 */
function replaceItemBatches_(ss, itemId, itemName, newBatches) {
  var existingSheet = ss.getSheetByName(BATCHES_SHEET_NAME);
  var cleanNew = [];
  for (var n = 0; n < (newBatches || []).length; n++) {
    var nb = newBatches[n];
    var q = parseQuantity_(nb.qty);
    if (q <= 0) continue;
    cleanNew.push({
      batchId: nb.batchId || makeBatchId_(),
      qty: q,
      expiry: nb.expiry || null,
      addedAt: nb.addedAt || new Date()
    });
  }
  sortBatchesFifo_(cleanNew);

  if (!existingSheet && cleanNew.length === 0) {
    return;
  }

  var sheet = ensureBatchesSheet_(ss);
  var tz = ss.getSpreadsheetTimeZone();
  var targetNameKey = normalizeKey_(itemName);
  var keptRows = [];
  var oldLastRow = sheet.getLastRow();

  if (oldLastRow > 1) {
    var oldVals = sheet.getRange(2, 1, oldLastRow - 1, BATCHES_HEADERS.length).getValues();
    for (var i = 0; i < oldVals.length; i++) {
      var r = oldVals[i];
      var rItemId = String(r[1] || '').trim();
      var rItemNameKey = normalizeKey_(r[2]);
      if ((itemId && rItemId === itemId) || (targetNameKey && rItemNameKey === targetNameKey)) {
        continue;
      }
      if (parseQuantity_(r[3]) <= 0) continue;
      keptRows.push(r);
    }
  }

  for (var k = 0; k < cleanNew.length; k++) {
    var b = cleanNew[k];
    keptRows.push([
      b.batchId,
      itemId || '',
      itemName || '',
      b.qty,
      expiryObjToSheetDate_(b.expiry),
      b.addedAt instanceof Date ? b.addedAt : (b.addedAt || formatSyncTimestamp_(new Date(), tz))
    ]);
  }

  if (keptRows.length > 0) {
    sheet.getRange(2, 1, keptRows.length, BATCHES_HEADERS.length).setValues(keptRows);
  }
  var newLastRow = keptRows.length + 1;
  if (oldLastRow > newLastRow) {
    sheet.deleteRows(newLastRow + 1, oldLastRow - newLastRow);
  }
  SpreadsheetApp.flush();
}

function addOrMergeBatch_(batches, qtyToAdd, expiry, nowDate) {
  var list = [];
  var merged = false;
  var targetKey = expiryKey_(expiry);

  for (var i = 0; i < batches.length; i++) {
    var b = {
      batchId: batches[i].batchId || makeBatchId_(),
      qty: parseQuantity_(batches[i].qty),
      expiry: batches[i].expiry || null,
      addedAt: batches[i].addedAt || ''
    };
    if (!merged && expiryKey_(b.expiry) === targetKey) {
      b.qty = roundQty_(b.qty + qtyToAdd);
      merged = true;
    }
    if (b.qty > 0) list.push(b);
  }

  if (!merged && qtyToAdd > 0) {
    list.push({
      batchId: makeBatchId_(),
      qty: roundQty_(qtyToAdd),
      expiry: expiry || null,
      addedAt: nowDate || new Date()
    });
  }

  sortBatchesFifo_(list);
  return {
    batches: list,
    totalQty: sumBatchesQty_(list),
    soonestExpiry: list.length > 0 ? list[0].expiry : null
  };
}

function deductBatchesFifo_(batches, qtyToUse) {
  var sorted = sortBatchesFifo_(batches.slice());
  var remainingToUse = parseQuantity_(qtyToUse);
  var remainingBatches = [];

  for (var i = 0; i < sorted.length; i++) {
    var bQty = parseQuantity_(sorted[i].qty);
    if (bQty <= 0) continue;

    if (remainingToUse > 0) {
      if (bQty <= remainingToUse) {
        remainingToUse = roundQty_(remainingToUse - bQty);
      } else {
        remainingBatches.push({
          batchId: sorted[i].batchId,
          qty: roundQty_(bQty - remainingToUse),
          expiry: sorted[i].expiry || null,
          addedAt: sorted[i].addedAt || ''
        });
        remainingToUse = 0;
      }
    } else {
      remainingBatches.push({
        batchId: sorted[i].batchId,
        qty: bQty,
        expiry: sorted[i].expiry || null,
        addedAt: sorted[i].addedAt || ''
      });
    }
  }

  sortBatchesFifo_(remainingBatches);
  return {
    remainingBatches: remainingBatches,
    totalQty: sumBatchesQty_(remainingBatches),
    soonestExpiry: remainingBatches.length > 0 ? remainingBatches[0].expiry : null
  };
}

/**
 * Computes "Days of Food" Family Readiness Estimator.
 */
function computeDaysOfFood_(items, settings) {
  var targetYears = (settings && settings.targetYears) ? Number(settings.targetYears) : 2;
  var targetDays = Math.round(targetYears * 365);
  var sumRatio = 0;
  var counted = 0;

  for (var i = 0; i < (items || []).length; i++) {
    var it = items[i];
    if (it && typeof it.target === 'number' && it.target > 0) {
      var ratio = Math.min(1, Math.max(0, Number(it.qty) || 0) / it.target);
      sumRatio += ratio;
      counted++;
    }
  }

  var avg = counted > 0 ? (sumRatio / counted) : 0;
  var readinessPct = Math.round(avg * 100);
  var estimatedDays = Math.round(avg * targetDays);

  var adults = (settings && settings.adults) ? settings.adults : 2;
  var kidsCount = (settings && Array.isArray(settings.kidAges)) ? settings.kidAges.length : 0;
  var familyText = adults + ' adult' + (adults === 1 ? '' : 's') +
    (kidsCount > 0 ? ' & ' + kidsCount + ' kid' + (kidsCount === 1 ? '' : 's') : '');

  return {
    estimatedDays: estimatedDays,
    targetDays: targetDays,
    readinessPct: readinessPct,
    summaryText: '~' + estimatedDays + ' days stocked for ' + familyText
  };
}

/**
 * Guesses best default category ID from product/item keywords.
 */
function guessCategoryFromKeywords_(text) {
  var s = String(text || '').toLowerCase();
  if (/\b(rice|flour|atta|oats?|pasta|noodles?|wheat|quinoa|cereal|bread|barley|couscous|semolina|macaroni|spaghetti)\b/.test(s)) return 'grains';
  if (/\b(daal|dal|lentils?|tuna|chicken|chickpeas?|beans?|meat|beef|salmon|sardines?|peanuts?|almonds?|nuts?|protein|tofu|eggs?|chana|rajma)\b/.test(s)) return 'proteins';
  if (/\b(formula|baby|toddler|pouch(?:es)?|milk|pediasure|gerber|puree|infant|similac|enfamil)\b/.test(s)) return 'baby';
  if (/\b(water|drink|drinks|filter|juice|electrolyte|hydration|beverage)\b/.test(s)) return 'water';
  if (/\b(diapers?|wipes?|meds?|medicine|tylenol|ibuprofen|advil|bandages?|soap|batter(?:y|ies)|first\s*aid|toothpaste|shampoo|detergent|tissue|toilet|bleach|sanitizer)\b/.test(s)) return 'essentials';
  if (/\b(oil|ghee|salt|sugar|spices?|masala|vinegar|honey|butter|turmeric|cumin|pepper|garlic|ginger|tea|coffee|baking|yeast)\b/.test(s)) return 'oils';
  if (/\b(tomato(?:es)?|veg(?:etables?)?|soup|fruit|canned|corn|peas|carrots?|pickles?|salsa|sauce|broth|stock|jam)\b/.test(s)) return 'canned';
  return 'grains';
}

function guessUnitFromQuantityText_(qtyText, nameText) {
  var s = (String(qtyText || '') + ' ' + String(nameText || '')).toLowerCase();
  if (/\b(lb|lbs|pound|pounds)\b/.test(s)) return 'lbs';
  if (/\b(kg|kilogram|kilograms)\b/.test(s)) return 'kg';
  if (/\b(gal|gallon|gallons)\b/.test(s)) return 'gallons';
  if (/\b(can|cans|tin|tins)\b/.test(s)) return 'cans';
  if (/\b(pouch|pouches)\b/.test(s)) return 'pouches';
  if (/\b(jar|jars)\b/.test(s)) return 'jars';
  if (/\b(bottle|bottles|ml|l|liter|liters|fl\s*oz)\b/.test(s)) return 'bottles';
  if (/\b(bag|bags)\b/.test(s)) return 'bags';
  if (/\b(box|boxes)\b/.test(s)) return 'boxes';
  return 'packs';
}

function normalizeUnitWord_(rawUnit) {
  var u = String(rawUnit || '').trim().toLowerCase();
  if (!u) return '';
  if (u === 'lb' || u === 'lbs' || u === 'pound' || u === 'pounds') return 'lbs';
  if (u === 'kg' || u === 'kgs' || u === 'kilogram' || u === 'kilograms') return 'kg';
  if (u === 'can' || u === 'cans') return 'cans';
  if (u === 'bag' || u === 'bags') return 'bags';
  if (u === 'gal' || u === 'gallon' || u === 'gallons') return 'gallons';
  if (u === 'box' || u === 'boxes') return 'boxes';
  if (u === 'pack' || u === 'packs' || u === 'pkg' || u === 'pkgs') return 'packs';
  if (u === 'jar' || u === 'jars') return 'jars';
  if (u === 'bottle' || u === 'bottles') return 'bottles';
  if (u === 'pouch' || u === 'pouches') return 'pouches';
  return u;
}

/**
 * Matches an item name against existing items using fuzzy keys + strong token overlap
 * (so e.g. "tuna" matches "Canned Tuna" and "diapers" matches "Diapers & Baby Wipes").
 */
function findBestMatchingItem_(items, queryName) {
  if (!Array.isArray(items) || !queryName) return null;
  var queryKeys = buildFuzzyItemKeys_(queryName);
  var keySet = {};
  for (var k = 0; k < queryKeys.length; k++) {
    keySet[queryKeys[k]] = true;
  }

  // Pass 1: Exact fuzzy key match
  for (var i = 0; i < items.length; i++) {
    var itemKeys = buildFuzzyItemKeys_(items[i].name);
    for (var j = 0; j < itemKeys.length; j++) {
      if (keySet[itemKeys[j]]) return items[i];
    }
  }

  // Pass 2: Singularized token overlap (every query token matches inside item tokens)
  var cleanQ = normalizeKey_(queryName).replace(/[^a-z0-9\s]/g, ' ').trim();
  var qTokens = [];
  var rawQTokens = cleanQ.split(/\s+/);
  for (var qt = 0; qt < rawQTokens.length; qt++) {
    if (rawQTokens[qt] && rawQTokens[qt].length >= 3) {
      qTokens.push(singularizeToken_(rawQTokens[qt]));
    }
  }
  if (qTokens.length === 0) return null;

  var bestItem = null;
  var bestScore = 0;
  for (var m = 0; m < items.length; m++) {
    var cleanItem = normalizeKey_(items[m].name).replace(/[^a-z0-9\s]/g, ' ').trim();
    var iTokens = cleanItem.split(/\s+/).map(singularizeToken_);
    var allMatch = true;
    for (var t = 0; t < qTokens.length; t++) {
      if (iTokens.indexOf(qTokens[t]) === -1) {
        allMatch = false;
        break;
      }
    }
    if (allMatch) {
      var score = qTokens.length / Math.max(iTokens.length, 1);
      if (score > bestScore) {
        bestScore = score;
        bestItem = items[m];
      }
    }
  }

  return bestItem;
}

/**
 * Parses multi-item natural language pantry text into structured quick entries.
 * Example: "bought 20 lbs basmati rice exp may 2028, used 2 tuna, bought 3 packs diapers"
 */
function parseQuickPantryText_(text, items, categories, tz) {
  var raw = String(text || '').trim();
  if (!raw) return [];

  // Protect dates like "May 15, 2028" from comma splitting
  var protectedText = raw.replace(/(\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\.?\s+\d{1,2}),\s*(\d{4})/gi, '$1 $2');

  // Split by newlines, semicolons, commas, or " and " before an action verb or number
  var clauses = protectedText.split(/(?:[\n\r;,]+|\s+and\s+(?=(?:used|use|took|take|ate|eat|finished|bought|buy|got|added|add|restocked|[+\-]?\d)))/i);

  var results = [];
  var lastAction = 'add';

  for (var i = 0; i < clauses.length; i++) {
    var clause = String(clauses[i] || '').trim();
    if (!clause) continue;

    var action = lastAction;
    if (/^(?:\-\s*|\b(?:used|use|took|take|ate|eat|finished|finish|consumed|consume|opened|open|removed|remove)\b)/i.test(clause)) {
      action = 'use';
      clause = clause.replace(/^(?:\-\s*|\b(?:used|use|took|take|ate|eat|finished|finish|consumed|consume|opened|open|removed|remove)\b\s*)/i, '').trim();
    } else if (/^(?:\+\s*|\b(?:bought|buy|got|get|added|add|restocked|restock|stocked|purchased|picked\s+up)\b)/i.test(clause)) {
      action = 'add';
      clause = clause.replace(/^(?:\+\s*|\b(?:bought|buy|got|get|added|add|restocked|restock|stocked|purchased|picked\s+up)\b\s*)/i, '').trim();
    }
    lastAction = action;

    // Extract optional expiry phrase
    var expiry = null;
    var expRegex = /(?:\b(?:exp(?:ires|iry|iration)?|best\s*by|use\s*by)\b[:\s-]*)((?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?\s+\d{1,2}(?:st|nd|rd|th)?\s+\d{2,4}|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\.?\s+\d{4}|\d{4}[\/\-]\d{1,2}[\/\-]\d{1,2}|\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4}|\d{1,2}[\/\-]\d{4})/i;
    var expMatch = clause.match(expRegex);
    if (expMatch) {
      expiry = parseExpiryValue_(expMatch[1], tz);
      clause = clause.replace(expMatch[0], ' ').replace(/\s+/g, ' ').trim();
    }

    // Extract quantity + optional unit (at start or after name)
    var qty = 0;
    var unit = '';
    var namePart = clause;
    var leadQtyMatch = clause.match(/^(\d+(?:\.\d+)?)\s*(lbs?|pounds?|kgs?|kilograms?|cans?|bags?|gallons?|gal|boxes?|box|packs?|pkgs?|jars?|bottles?|pouches?|pouch)?\b\s*(.*)$/i);
    if (leadQtyMatch) {
      qty = parseQuantity_(leadQtyMatch[1]);
      unit = normalizeUnitWord_(leadQtyMatch[2] || '');
      namePart = String(leadQtyMatch[3] || '').trim();
    } else {
      var trailQtyMatch = clause.match(/^(.+?)\s+(\d+(?:\.\d+)?)\s*(lbs?|pounds?|kgs?|kilograms?|cans?|bags?|gallons?|gal|boxes?|box|packs?|pkgs?|jars?|bottles?|pouches?|pouch)?$/i);
      if (trailQtyMatch) {
        namePart = String(trailQtyMatch[1] || '').trim();
        qty = parseQuantity_(trailQtyMatch[2]);
        unit = normalizeUnitWord_(trailQtyMatch[3] || '');
      }
    }

    // Clean filler words from item name
    namePart = namePart.replace(/^(?:of|more|some|the|a|an)\s+/i, '').replace(/[.,!?]+$/, '').trim();
    if (!namePart || qty <= 0) continue;

    var matched = findBestMatchingItem_(items, namePart);
    var displayName = matched ? matched.name : titleCaseSimple_(namePart);
    var catId = matched ? matched.category : guessCategoryFromKeywords_(namePart);
    var finalUnit = unit || (matched ? matched.unit : '') || 'packs';

    results.push({
      action: action,
      matchedItemId: matched ? matched.id : null,
      name: displayName,
      category: catId,
      qty: qty,
      unit: finalUnit,
      target: matched ? matched.target : Math.max(qty, defaultTargetForCategory_(catId)),
      expiry: expiry,
      location: matched ? matched.location : '',
      notes: ''
    });
  }

  return results;
}

function titleCaseSimple_(str) {
  return String(str || '').replace(/\b[a-z]/g, function (ch) { return ch.toUpperCase(); });
}

function getWebAppUrlSafe_() {
  try {
    if (typeof ScriptApp !== 'undefined' && ScriptApp && typeof ScriptApp.getService === 'function') {
      return ScriptApp.getService().getUrl() || '';
    }
  } catch (e) {
    return '';
  }
  return '';
}

function buildTelegramHomeKeyboard_(appUrl) {
  var rows = [
    [
      { text: "🛒 Groceries", callback_data: "groceries" },
      { text: "📋 Alerts", callback_data: "alerts" }
    ],
    [
      { text: "📦 Emergency Shop", callback_data: "shop" },
      { text: "🍽️ Days of Food", callback_data: "readiness" }
    ]
  ];
  if (appUrl && appUrl.indexOf("https://") === 0) {
    rows.push([{ text: "🏠 Open Full App", url: appUrl }]);
  }
  return { inline_keyboard: rows };
}

function buildTelegramAlertKeyboard_(appUrl) {
  var rows = [
    [
      { text: "🛒 Groceries", callback_data: "groceries" },
      { text: "📋 Alerts", callback_data: "alerts" }
    ]
  ];
  if (appUrl && appUrl.indexOf("https://") === 0) {
    rows.push([{ text: "🏠 Open Full Pantry App", url: appUrl }]);
  }
  return { inline_keyboard: rows };
}

function parseQuickItemPiece_(text) {
  var clean = String(text || "").trim();
  if (!clean) return null;
  var m = clean.match(/^(\d+(?:\.\d+)?)\s*([a-zA-Z]+)?\s+(.+)$/);
  if (m) {
    var qty = Number(m[1]);
    var potentialUnit = (m[2] || "").toLowerCase();
    var rest = m[3].trim();
    var knownUnits = { lbs: 1, lb: 1, kg: 1, g: 1, oz: 1, gallons: 1, gal: 1, liters: 1, l: 1, cans: 1, can: 1, boxes: 1, box: 1, bags: 1, bag: 1, packs: 1, pack: 1, bottles: 1, bottle: 1, jars: 1, jar: 1, tubs: 1, tub: 1, bunches: 1, bunch: 1, loaves: 1, loaf: 1 };
    if (knownUnits[potentialUnit]) {
      return { qty: qty, unit: potentialUnit, name: rest };
    } else if (m[2]) {
      return { qty: qty, unit: "", name: (m[2] + " " + rest).trim() };
    } else {
      return { qty: qty, unit: "", name: rest };
    }
  }
  return { qty: 1, unit: "", name: clean };
}

function buildTelegramGroceriesText_(ss) {
  var groceries = readGroceriesInternal_(ss);
  var unchecked = [];
  for (var i = 0; i < groceries.length; i++) {
    if (!groceries[i].checked) unchecked.push(groceries[i]);
  }

  if (unchecked.length === 0) {
    return "🛒 *Weekly Groceries — All Clear!* 🎉\n\n" +
      "Your grocery list has no unchecked items.\n\n" +
      "Add items anytime right here in Telegram:\n" +
      "• `buy milk, eggs, bread`\n" +
      "• `need 10 lbs halal chicken, yogurt`";
  }

  var byStore = {};
  for (var u = 0; u < unchecked.length; u++) {
    var item = unchecked[u];
    var st = item.store || guessGroceryStore_(item.name, item.aisle) || "Grocery Store";
    if (!byStore[st]) byStore[st] = [];
    byStore[st].push(item);
  }

  var lines = [
    "🛒 *Weekly Groceries (" + unchecked.length + " item" + (unchecked.length === 1 ? "" : "s") + " to buy)*",
    ""
  ];

  var storeNames = Object.keys(byStore);
  for (var s = 0; s < storeNames.length; s++) {
    var storeName = storeNames[s];
    var storeItems = byStore[storeName];
    var storeIcon = "🛒";
    if (/costco/i.test(storeName)) storeIcon = "🏬";
    else if (/halal/i.test(storeName)) storeIcon = "🥩";
    else if (/target/i.test(storeName)) storeIcon = "🎯";

    lines.push("*" + storeIcon + " " + storeName + " (" + storeItems.length + ")*");
    for (var k = 0; k < storeItems.length; k++) {
      var it = storeItems[k];
      var qtyStr = (it.qty > 1 || it.unit) ? " (" + it.qty + (it.unit ? " " + it.unit : "") + ")" : "";
      lines.push("  • " + it.name + qtyStr);
    }
    lines.push("");
  }

  lines.push("_Type `buy <items>` to add more!_");
  return lines.join("\n");
}
function sendTelegramMessage_(botToken, chatId, text, replyMarkup) {
  var payload = {
    chat_id: String(chatId),
    text: String(text || ''),
    parse_mode: 'Markdown',
    disable_web_page_preview: true
  };
  if (replyMarkup) {
    payload.reply_markup = replyMarkup;
  }
  var res = UrlFetchApp.fetch('https://api.telegram.org/bot' + encodeURIComponent(botToken) + '/sendMessage', {
    method: 'post',
    contentType: 'application/json',
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  });
  var code = typeof res.getResponseCode === 'function' ? res.getResponseCode() : 200;
  return code >= 200 && code < 300;
}

function answerTelegramCallback_(botToken, callbackQueryId, text) {
  try {
    UrlFetchApp.fetch('https://api.telegram.org/bot' + encodeURIComponent(botToken) + '/answerCallbackQuery', {
      method: 'post',
      contentType: 'application/json',
      payload: JSON.stringify({ callback_query_id: String(callbackQueryId), text: String(text || '') }),
      muteHttpExceptions: true
    });
  } catch (e) {
    // Non-fatal
  }
}

function buildTelegramShoppingListText_(ss, settings) {
  var info = findOrCreateInventorySheet_(ss, false);
  if (info.isNewSheet) {
    return '🛒 *Auto Shopping List*\n\nYour pantry sheet is empty right now.';
  }
  var data = readPantryDataInternal_(ss, info);
  var needed = [];
  for (var i = 0; i < data.items.length; i++) {
    var it = data.items[i];
    if (it.target > 0) {
      var pct = Math.round((it.qty / it.target) * 100);
      if (pct < settings.lowStockPct) {
        needed.push({ item: it, pct: pct, need: Math.max(1, roundQty_(it.target - it.qty)) });
      }
    }
  }
  needed.sort(function (a, b) { return a.pct - b.pct; });

  if (needed.length === 0) {
    return '🛒 *Auto Shopping List*\n\n✅ All items are above ' + settings.lowStockPct + '% of your ' + settings.targetYears + '-year goal!';
  }

  var lines = ['🛒 *Auto Shopping List (' + needed.length + ' items below ' + settings.lowStockPct + '%)*', ''];
  for (var n = 0; n < Math.min(needed.length, 20); n++) {
    var entry = needed[n];
    var uStr = entry.item.unit ? ' ' + entry.item.unit : '';
    lines.push('• *' + entry.item.name + '* — buy +' + entry.need + uStr + ' (have ' + entry.item.qty + '/' + entry.item.target + uStr + ')');
  }
  if (needed.length > 20) {
    lines.push('• ...and ' + (needed.length - 20) + ' more in the full app');
  }
  return lines.join('\n');
}

function buildTelegramReadinessText_(ss, settings) {
  var info = findOrCreateInventorySheet_(ss, false);
  if (info.isNewSheet) {
    return '🍽️ *Days of Food*\n\nNo pantry items tracked yet.';
  }
  var data = readPantryDataInternal_(ss, info);
  var dof = data.daysOfFood;
  return (
    '🍽️ *Family Readiness Estimator*\n\n' +
    '• *' + dof.summaryText + '*\n' +
    '• Goal: ' + dof.targetDays + ' days (' + settings.targetYears + ' years)\n' +
    '• Overall Readiness: *' + dof.readinessPct + '%*\n' +
    '• Items Tracked: ' + data.items.length
  );
}

function sendTelegramItemCheck_(ss, botToken, chatId, query, appUrl) {
  var info = findOrCreateInventorySheet_(ss, false);
  if (info.isNewSheet) {
    sendTelegramMessage_(botToken, chatId, 'No items tracked yet.', buildTelegramHomeKeyboard_(appUrl));
    return;
  }
  var data = readPantryDataInternal_(ss, info);
  var matched = findBestMatchingItem_(data.items, query);
  if (!matched) {
    sendTelegramMessage_(botToken, chatId, '🔍 Could not find "' + query + '" in your pantry.', buildTelegramHomeKeyboard_(appUrl));
    return;
  }

  var uStr = matched.unit ? ' ' + matched.unit : '';
  var lines = [
    '📦 *' + matched.name + '*',
    '• Stock: *' + matched.qty + uStr + '*' + (matched.target > 0 ? ' / ' + matched.target + uStr + ' goal' : ''),
    '• Spot: ' + (matched.location || 'Not set')
  ];
  if (matched.batches && matched.batches.length > 0) {
    lines.push('• Batches (oldest first):');
    for (var b = 0; b < matched.batches.length; b++) {
      var bt = matched.batches[b];
      var expTxt = bt.expiry
        ? (MONTH_SHORT_NAMES[bt.expiry.m] + ' ' + bt.expiry.d + ', ' + bt.expiry.y)
        : 'No Expiry';
      lines.push('  ' + (b + 1) + '. ' + bt.qty + uStr + ' — ' + expTxt);
    }
  }
  if (matched.notes) {
    lines.push('• Notes: ' + matched.notes);
  }

  var kb = {
    inline_keyboard: [
      [
        { text: '➖ Use 1', callback_data: 'adj:' + matched.id + ':-1' },
        { text: '➕ Add 1', callback_data: 'adj:' + matched.id + ':1' }
      ],
      [
        { text: '📋 Alerts', callback_data: 'alerts' },
        { text: '🛒 Shopping List', callback_data: 'shop' }
      ]
    ]
  };
  sendTelegramMessage_(botToken, chatId, lines.join('\n'), kb);
}

/**
 * Parses comma/semicolon/newline-separated US phone numbers into deduplicated
 * Google Fi SMS gateway addresses (<10digits>@msg.fi.google.com).
 */
function parseGoogleFiAddresses_(phonesStr) {
  var raw = String(phonesStr === null || phonesStr === undefined ? '' : phonesStr).trim();
  if (!raw) return [];

  var parts = raw.split(/[,;\n\r]+/);
  var addrs = [];
  var seen = {};

  for (var i = 0; i < parts.length; i++) {
    var digits = parts[i].replace(/\D/g, '');
    if (digits.length === 11 && digits.charAt(0) === '1') {
      digits = digits.slice(1);
    }
    if (digits.length === 10) {
      var addr = digits + '@msg.fi.google.com';
      if (!seen[addr]) {
        seen[addr] = true;
        addrs.push(addr);
      }
    }
  }

  return addrs;
}

function escapeHtml_(str) {
  return String(str === null || str === undefined ? '' : str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function cloneCategoriesList_(list) {
  var out = [];
  for (var i = 0; i < list.length; i++) {
    var c = list[i];
    out.push({
      id: c.id,
      name: c.name,
      color: c.color,
      soft: c.soft,
      icon: c.icon
    });
  }
  return out;
}

/* ==========================================================================
   SMART SHEET AUTO-DETECTION & COLUMN MAPPING
   ========================================================================== */

/**
 * Scans all sheets in the active spreadsheet to locate an existing inventory tab,
 * or creates a clean styled "Pantry Inventory" tab if none exists.
 *
 * Existing tabs & Google Sheets Tables are NEVER mutated or restructured.
 */
function findOrCreateInventorySheet_(ss, allowMutate) {
  var sheets = ss.getSheets();
  var bestMatch = null;

  for (var i = 0; i < sheets.length; i++) {
    var sheet = sheets[i];
    if (shouldSkipSheetName_(sheet.getName())) {
      continue;
    }

    var detection = detectInventoryHeader_(sheet);
    if (!detection) {
      continue;
    }

    var isPreferredName = sheet.getName().trim().toLowerCase() === DEFAULT_INVENTORY_SHEET_NAME.toLowerCase();
    if (
      !bestMatch ||
      (isPreferredName && !bestMatch.isPreferredName) ||
      (isPreferredName === bestMatch.isPreferredName && detection.score > bestMatch.score)
    ) {
      bestMatch = {
        sheet: sheet,
        headerRow: detection.headerRow,
        colMap: detection.colMap,
        score: detection.score,
        isPreferredName: isPreferredName,
        isNewSheet: false
      };
    }
  }

  if (bestMatch) {
    return bestMatch;
  }

  // No matching inventory tab found
  if (!allowMutate) {
    var existingNamed = ss.getSheetByName(DEFAULT_INVENTORY_SHEET_NAME);
    return {
      sheet: existingNamed || sheets[0],
      headerRow: 1,
      colMap: buildCanonicalColMap_(),
      isNewSheet: true
    };
  }

  return createFreshInventorySheet_(ss);
}

/**
 * Checks whether a tab name should be ignored during inventory auto-detection.
 */
function shouldSkipSheetName_(name) {
  var lower = String(name || '').trim().toLowerCase();
  if (SKIPPED_SHEET_NAMES[lower]) return true;
  if (lower.indexOf('form responses') === 0) return true;
  return false;
}

/**
 * Scans rows 1–5 of a sheet to find a valid inventory header row.
 * Requires an item/name column AND at least one inventory column
 * (category, qty, target, or expiry).
 */
function detectInventoryHeader_(sheet) {
  var lastRow = sheet.getLastRow();
  var lastCol = sheet.getLastColumn();
  if (lastRow === 0 || lastCol === 0) return null;

  var scanRows = Math.min(5, lastRow);
  var values = sheet.getRange(1, 1, scanRows, lastCol).getDisplayValues();

  var bestRowResult = null;
  for (var r = 0; r < scanRows; r++) {
    var row = values[r];
    var colMap = mapHeaderRow_(row);

    var hasName = colMap.name !== null;
    var inventoryColCount =
      (colMap.category !== null ? 1 : 0) +
      (colMap.qty !== null ? 1 : 0) +
      (colMap.target !== null ? 1 : 0) +
      (colMap.expiry !== null ? 1 : 0);

    if (hasName && inventoryColCount >= 1) {
      var score =
        inventoryColCount * 10 +
        (colMap.id !== null ? 4 : 0) +
        (colMap.unit !== null ? 3 : 0) +
        (colMap.location !== null ? 2 : 0) +
        (colMap.notes !== null ? 2 : 0) +
        (colMap.lastUpdated !== null ? 2 : 0);

      if (!bestRowResult || score > bestRowResult.score) {
        bestRowResult = {
          headerRow: r + 1,
          colMap: colMap,
          score: score
        };
      }
    }
  }

  return bestRowResult;
}

/**
 * Maps a header row array into 1-based column indices.
 */
function mapHeaderRow_(rowValues) {
  var colMap = {
    id: null,
    category: null,
    name: null,
    qty: null,
    target: null,
    unit: null,
    expiry: null,
    location: null,
    notes: null,
    lastUpdated: null
  };

  var usedCols = {};

  function claimColumn(key, predicate) {
    if (colMap[key] !== null) return;
    for (var c = 0; c < rowValues.length; c++) {
      if (usedCols[c]) continue;
      var h = normalizeHeader_(rowValues[c]);
      if (!h) continue;
      if (predicate(h)) {
        colMap[key] = c + 1;
        usedCols[c] = true;
        return;
      }
    }
  }

  // Pass 1: Specific / unambiguous matches first so e.g. "Last Updated" never steals "Expiration Date"
  claimColumn('lastUpdated', function (h) {
    return h === 'last updated' || h === 'updated' || h === 'updated at' || h === 'last modified' || h === 'modified' || h === 'timestamp' || h.indexOf('last updated') !== -1;
  });

  claimColumn('id', function (h) {
    return h === 'id' || h === 'item id' || h === 'row id' || h === 'pantry id' || h === 'sku' || h === 'code';
  });

  claimColumn('category', function (h) {
    return h === 'category' || h === 'type' || h === 'group' || h === 'food group' || h === 'section' || h === 'aisle' || h === 'cat' ||
      h.indexOf('category') !== -1;
  });

  claimColumn('target', function (h) {
    return h === '3-year goal' || h === '3 year goal' || h === '3-yr goal' || h === '3yr goal' ||
      h === 'target' || h === 'goal' || h === 'needed' || h === 'par' || h === 'par level' ||
      h === 'target qty' || h === 'target stock' || h === 'goal qty' || h === 'desired' || h === 'required' ||
      h.indexOf('target') !== -1 || h.indexOf('goal') !== -1 || h.indexOf('needed') !== -1;
  });

  claimColumn('expiry', function (h) {
    return h === 'expiration date' || h === 'expiry' || h === 'expiration' || h === 'exp date' || h === 'exp' ||
      h === 'best by' || h === 'best before' || h === 'use by' || h === 'expires' || h === 'expiry date' ||
      h.indexOf('expir') !== -1 || h.indexOf('best by') !== -1 || h.indexOf('best before') !== -1 || h.indexOf('use by') !== -1;
  });

  claimColumn('qty', function (h) {
    return h === 'current stock' || h === 'qty' || h === 'quantity' || h === 'stock' || h === 'current' ||
      h === 'have' || h === 'on hand' || h === 'count' || h === 'in stock' || h === 'amount' || h === 'available' ||
      h === 'current qty' || h === 'qty on hand' ||
      h.indexOf('qty') !== -1 || h.indexOf('quantity') !== -1 || h.indexOf('stock') !== -1 || h.indexOf('on hand') !== -1 || h.indexOf('count') !== -1;
  });

  claimColumn('unit', function (h) {
    return h === 'unit' || h === 'units' || h === 'uom' || h === 'measure' || h === 'packaging' || h === 'pack size' || h === 'size' ||
      h.indexOf('unit') !== -1;
  });

  claimColumn('location', function (h) {
    return h === 'storage spot' || h === 'location' || h === 'storage' || h === 'storage location' ||
      h === 'where' || h === 'shelf' || h === 'bin' || h === 'place' || h === 'area' || h === 'spot' ||
      h.indexOf('location') !== -1 || h.indexOf('storage') !== -1;
  });

  claimColumn('notes', function (h) {
    return h === 'notes' || h === 'note' || h === 'comments' || h === 'comment' || h === 'remarks' || h === 'details' ||
      h.indexOf('note') !== -1 || h.indexOf('comment') !== -1;
  });

  claimColumn('name', function (h) {
    return h === 'item name' || h === 'item' || h === 'food' || h === 'food item' || h === 'product' ||
      h === 'product name' || h === 'name' || h === 'supply' || h === 'supply name' || h === 'ration' || h === 'ration name' ||
      h.indexOf('item') !== -1 || h.indexOf('food') !== -1 || h.indexOf('product') !== -1 ||
      h.indexOf('supply') !== -1 || h.indexOf('ration') !== -1 || h.indexOf('name') !== -1;
  });

  // Pass 2: Fallback for plain "Date" column as expiry if expiry is still unmapped
  claimColumn('expiry', function (h) {
    return h === 'date' || h.indexOf('date') !== -1;
  });

  // Pass 3: Fallback for "Type" / "Group" substring if category still unmapped
  claimColumn('category', function (h) {
    return h.indexOf('type') !== -1 || h.indexOf('group') !== -1;
  });

  // Pass 4: Fallback for "Have" / "Current" substring if qty still unmapped
  claimColumn('qty', function (h) {
    return h.indexOf('have') !== -1 || h.indexOf('current') !== -1;
  });

  return colMap;
}

/**
 * Creates a clean styled "Pantry Inventory" sheet with standard headers.
 */
function createFreshInventorySheet_(ss) {
  var sheet = ss.getSheetByName(DEFAULT_INVENTORY_SHEET_NAME);
  if (sheet && sheet.getLastRow() === 0 && sheet.getLastColumn() === 0) {
    // Reuse empty Pantry Inventory tab
  } else if (sheet) {
    // Pantry Inventory exists with unrelated content; create a fresh unique tab
    var suffix = 2;
    while (ss.getSheetByName(DEFAULT_INVENTORY_SHEET_NAME + ' ' + suffix)) {
      suffix++;
    }
    sheet = ss.insertSheet(DEFAULT_INVENTORY_SHEET_NAME + ' ' + suffix, 0);
  } else {
    sheet = ss.insertSheet(DEFAULT_INVENTORY_SHEET_NAME, 0);
  }

  var titles = [];
  var colMap = {};
  for (var i = 0; i < CANONICAL_HEADERS.length; i++) {
    titles.push(CANONICAL_HEADERS[i].title);
    colMap[CANONICAL_HEADERS[i].key] = i + 1;
  }

  var headerRange = sheet.getRange(1, 1, 1, titles.length);
  headerRange.setValues([titles]);
  SpreadsheetApp.flush();

  try {
    headerRange
      .setBackground(HEADER_BG_COLOR)
      .setFontColor(HEADER_TEXT_COLOR)
      .setFontWeight('bold')
      .setFontSize(10)
      .setVerticalAlignment('middle');
    sheet.setRowHeight(1, 34);
    sheet.setFrozenRows(1);

    for (var c = 0; c < CANONICAL_HEADERS.length; c++) {
      var spec = CANONICAL_HEADERS[c];
      sheet.setColumnWidth(c + 1, spec.width);
      sheet.getRange(1, c + 1).setHorizontalAlignment(spec.align);
    }

    var maxDataRows = Math.max(sheet.getMaxRows() - 1, 1);
    sheet.getRange(2, colMap.qty, maxDataRows, 1).setHorizontalAlignment('center');
    sheet.getRange(2, colMap.target, maxDataRows, 1).setHorizontalAlignment('center');
    sheet.getRange(2, colMap.unit, maxDataRows, 1).setHorizontalAlignment('center');
    sheet.getRange(2, colMap.expiry, maxDataRows, 1).setHorizontalAlignment('center');
    sheet.getRange(2, colMap.lastUpdated, maxDataRows, 1).setHorizontalAlignment('center');
    SpreadsheetApp.flush();
  } catch (styleErr) {
    // Ignore cosmetic styling errors; header values are already committed
  }

  return {
    sheet: sheet,
    headerRow: 1,
    colMap: colMap,
    isNewSheet: true
  };
}

function buildCanonicalColMap_() {
  var map = {};
  for (var i = 0; i < CANONICAL_HEADERS.length; i++) {
    map[CANONICAL_HEADERS[i].key] = i + 1;
  }
  return map;
}

/* ==========================================================================
   READING ITEMS & CATEGORIES (100% READ-ONLY)
   ========================================================================== */

/**
 * Reads all inventory rows and builds the combined categories list (Settings categories + any custom sheet categories).
 * Never mutates the sheet; rows without a stored ID receive deterministic 'row-<rowIndex>' IDs.
 */
function readSheetInventory_(ss, info, baseCategories) {
  var initialCats = (Array.isArray(baseCategories) && baseCategories.length > 0)
    ? cloneCategoriesList_(baseCategories)
    : cloneCategoriesList_(readSettingsInternal_(ss).categories);
  var categories = initialCats.length > 0 ? initialCats : cloneDefaultCategories_();
  var categoryMap = {};
  for (var c = 0; c < categories.length; c++) {
    categoryMap[categories[c].id] = categories[c];
  }

  var sheet = info.sheet;
  var lastRow = sheet.getLastRow();
  var lastCol = sheet.getLastColumn();
  if (lastRow <= info.headerRow || lastCol === 0) {
    return { categories: categories, categoryMap: categoryMap, items: [] };
  }

  var tz = ss.getSpreadsheetTimeZone();
  var numRows = lastRow - info.headerRow;
  var range = sheet.getRange(info.headerRow + 1, 1, numRows, lastCol);
  var rawValues = range.getValues();
  var formulas = range.getFormulas();

  var colMap = info.colMap;
  var items = [];
  var seenIds = {};

  for (var r = 0; r < numRows; r++) {
    var row = rawValues[r];
    var fRow = formulas[r];
    var sheetRowIndex = info.headerRow + 1 + r;

    var rawName = colMap.name ? row[colMap.name - 1] : '';
    var name = String(rawName === null || rawName === undefined ? '' : rawName).trim();
    if (!name) continue;

    // Skip summary/total rows containing formulas (e.g. "Total" with =SUM(...))
    if (isSummaryRow_(name, fRow, colMap)) continue;

    var rawId = colMap.id ? String(row[colMap.id - 1] || '').trim() : '';
    var id = rawId;
    if (!id || seenIds[id]) {
      id = 'row-' + sheetRowIndex;
    }
    seenIds[id] = true;

    var rawCategory = colMap.category ? String(row[colMap.category - 1] || '').trim() : '';
    var catObj = resolveCategory_(rawCategory, categories, categoryMap) || DEFAULT_CATEGORIES[0];

    var rawQty = colMap.qty ? row[colMap.qty - 1] : 0;
    var qty = parseQuantity_(rawQty);

    var rawTarget = colMap.target ? row[colMap.target - 1] : '';
    var parsedTarget = parseOptionalNonNegativeNumber_(rawTarget);
    var target = parsedTarget !== undefined ? parsedTarget : 0;

    var rawUnit = colMap.unit ? String(row[colMap.unit - 1] || '').trim() : '';
    var unit = rawUnit || extractUnitFromQuantityString_(rawQty);

    var rawExpiry = colMap.expiry ? row[colMap.expiry - 1] : '';
    var expiry = parseExpiryValue_(rawExpiry, tz);

    var location = colMap.location ? String(row[colMap.location - 1] || '').trim() : '';
    var notes = colMap.notes ? String(row[colMap.notes - 1] || '').trim() : '';

    items.push({
      id: id,
      category: catObj.id,
      name: name,
      qty: qty,
      target: target,
      unit: unit,
      expiry: expiry,
      location: location,
      notes: notes,
      _rowIndex: sheetRowIndex
    });
  }

  return {
    categories: categories,
    categoryMap: categoryMap,
    items: items
  };
}

/**
 * Finds the next safe row to append a new inventory item.
 * If the bottom row is a Total/Summary row with formulas, inserts above it.
 */
function getNextAppendRow_(info) {
  var sheet = info.sheet;
  var lastRow = sheet.getLastRow();
  if (lastRow <= info.headerRow) {
    return info.headerRow + 1;
  }

  var lastCol = Math.max(sheet.getLastColumn(), 1);
  var bottomRowRange = sheet.getRange(lastRow, 1, 1, lastCol);
  var bottomValues = bottomRowRange.getValues()[0];
  var bottomFormulas = bottomRowRange.getFormulas()[0];
  var bottomName = info.colMap.name ? String(bottomValues[info.colMap.name - 1] || '').trim() : '';
  var bottomFirstCol = String(bottomValues[0] || '').trim();

  if (isSummaryRow_(bottomName || bottomFirstCol, bottomFormulas, info.colMap)) {
    sheet.insertRowBefore(lastRow);
    return lastRow;
  }

  return lastRow + 1;
}

function isSummaryRowName_(name) {
  return /^(grand\s+)?(sub)?totals?$/i.test(String(name || '').trim());
}

function isSummaryRow_(name, formulaRow, colMap) {
  if (!isSummaryRowName_(name)) return false;
  var qtyFormula = colMap.qty ? String(formulaRow[colMap.qty - 1] || '') : '';
  var targetFormula = colMap.target ? String(formulaRow[colMap.target - 1] || '') : '';
  return qtyFormula.indexOf('=') === 0 || targetFormula.indexOf('=') === 0;
}

/* ==========================================================================
   CATEGORY RESOLUTION & FALLBACK PALETTE
   ========================================================================== */

function cloneDefaultCategories_() {
  var list = [];
  for (var i = 0; i < DEFAULT_CATEGORIES.length; i++) {
    var c = DEFAULT_CATEGORIES[i];
    list.push({
      id: c.id,
      name: c.name,
      color: c.color,
      soft: c.soft,
      icon: c.icon
    });
  }
  return list;
}

/**
 * Matches a raw category string (id or display name) to the 7 defaults or an
 * existing custom category. If unrecognized and non-empty, registers a new
 * custom category with a deterministic slug, color, soft tint, and icon.
 */
function resolveCategory_(rawCategory, categories, categoryMap) {
  var text = String(rawCategory === null || rawCategory === undefined ? '' : rawCategory).trim();
  if (!text) return null;

  var lower = text.toLowerCase();
  var stripped = stripLeadingDecorators_(text);
  var strippedLower = stripped.toLowerCase();
  var slug = slugify_(stripped || text);

  // 1. Direct ID match
  if (categoryMap[lower]) return categoryMap[lower];
  if (categoryMap[slug]) return categoryMap[slug];

  // 2. Display name match (case-insensitive, with or without leading emoji)
  for (var i = 0; i < categories.length; i++) {
    var c = categories[i];
    var cNameLower = c.name.toLowerCase();
    if (cNameLower === lower || cNameLower === strippedLower) {
      return c;
    }
    if (slugify_(c.name) === slug) {
      return c;
    }
  }

  // 3. Auto-register custom category so no existing sheet data is lost
  var customId = slug || 'custom';
  var suffix = 2;
  while (categoryMap[customId]) {
    customId = (slug || 'custom') + '-' + suffix;
    suffix++;
  }

  var paletteIndex = hashString_(customId) % CUSTOM_CATEGORY_PALETTE.length;
  var style = CUSTOM_CATEGORY_PALETTE[paletteIndex];
  var customCategory = {
    id: customId,
    name: stripped || text,
    color: style.color,
    soft: style.soft,
    icon: style.icon
  };

  categories.push(customCategory);
  categoryMap[customId] = customCategory;
  return customCategory;
}

function stripLeadingDecorators_(text) {
  return String(text || '')
    .replace(/^[^\w\d]+/u, '')
    .trim();
}

function hashString_(str) {
  var hash = 0;
  var s = String(str || '');
  for (var i = 0; i < s.length; i++) {
    hash = ((hash << 5) - hash) + s.charCodeAt(i);
    hash = hash & hash;
  }
  return Math.abs(hash);
}

/* ==========================================================================
   DATE PARSING & FORMATTING
   ========================================================================== */

/**
 * Parses a sheet cell value or RPC payload date into { y, m, d } (0-indexed month) or null.
 * Handles:
 *  - { y, m, d } objects
 *  - JavaScript Date instances (using spreadsheet timezone to prevent off-by-one shifts)
 *  - ISO strings (YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss)
 *  - MM/DD/YYYY, M/D/YYYY, MM-DD-YYYY
 *  - Month D, YYYY / D Month YYYY / Month YYYY (e.g., "May 2027")
 *  - Blank / "No Expiry" / "N/A" / "None" -> null
 */
function parseExpiryValue_(val, tz) {
  if (val === null || val === undefined || val === '') {
    return null;
  }

  // Already an { y, m, d } object
  if (typeof val === 'object' && !(val instanceof Date)) {
    var oy = Number(val.y);
    var om = Number(val.m);
    var od = Number(val.d);
    if (isFinite(oy) && isFinite(om) && isFinite(od)) {
      return makeValidExpiryObj_(Math.floor(oy), Math.floor(om), Math.floor(od));
    }
    return null;
  }

  // JavaScript Date instance from Google Sheets
  if (val instanceof Date) {
    if (isNaN(val.getTime())) return null;
    if (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.formatDate === 'function' && tz) {
      var iso = Utilities.formatDate(val, tz, 'yyyy-MM-dd');
      var parts = iso.split('-');
      return makeValidExpiryObj_(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
    }
    return makeValidExpiryObj_(val.getFullYear(), val.getMonth(), val.getDate());
  }

  var str = String(val).trim();
  if (!str || /^(no\s*expiry|none|n\/?a|never|indefinite|non-?perishable|-+|—|null)$/i.test(str)) {
    return null;
  }

  // 1. ISO: YYYY-MM-DD or YYYY/MM/DD (with optional time)
  var isoMatch = str.match(/^(\d{4})[\/\-](\d{1,2})[\/\-](\d{1,2})(?:[T\s].*)?$/);
  if (isoMatch) {
    return makeValidExpiryObj_(Number(isoMatch[1]), Number(isoMatch[2]) - 1, Number(isoMatch[3]));
  }

  // 2. Numeric: MM/DD/YYYY or M-D-YY
  var slashMatch = str.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{2,4})$/);
  if (slashMatch) {
    var p1 = Number(slashMatch[1]);
    var p2 = Number(slashMatch[2]);
    var yr = normalizeYear_(Number(slashMatch[3]));
    var month = p1;
    var day = p2;
    if (p1 > 12 && p2 <= 12) {
      day = p1;
      month = p2;
    }
    return makeValidExpiryObj_(yr, month - 1, day);
  }

  // 3. YYYY-MM or MM/YYYY
  var ymMatch = str.match(/^(\d{4})[\/\-](\d{1,2})$/);
  if (ymMatch) {
    return makeValidExpiryObj_(Number(ymMatch[1]), Number(ymMatch[2]) - 1, 1);
  }
  var myMatch = str.match(/^(\d{1,2})[\/\-](\d{4})$/);
  if (myMatch) {
    return makeValidExpiryObj_(Number(myMatch[2]), Number(myMatch[1]) - 1, 1);
  }

  // 4. "Month D, YYYY" or "Mon D YYYY"
  var mdyMatch = str.match(/^([a-zA-Z]+)\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{2,4})$/);
  if (mdyMatch) {
    var mIdx1 = lookupMonthName_(mdyMatch[1]);
    if (mIdx1 !== null) {
      return makeValidExpiryObj_(normalizeYear_(Number(mdyMatch[3])), mIdx1, Number(mdyMatch[2]));
    }
  }

  // 5. "D Month YYYY"
  var dmyMatch = str.match(/^(\d{1,2})(?:st|nd|rd|th)?\s+([a-zA-Z]+)\.?,?\s+(\d{2,4})$/);
  if (dmyMatch) {
    var mIdx2 = lookupMonthName_(dmyMatch[2]);
    if (mIdx2 !== null) {
      return makeValidExpiryObj_(normalizeYear_(Number(dmyMatch[3])), mIdx2, Number(dmyMatch[1]));
    }
  }

  // 6. "Month YYYY" (e.g. "May 2027", "Sept 2028")
  var monthYearMatch = str.match(/^([a-zA-Z]+)\.?,?\s+(\d{4})$/);
  if (monthYearMatch) {
    var mIdx3 = lookupMonthName_(monthYearMatch[1]);
    if (mIdx3 !== null) {
      return makeValidExpiryObj_(Number(monthYearMatch[2]), mIdx3, 1);
    }
  }

  // 7. Native fallback
  var parsedDate = new Date(str);
  if (!isNaN(parsedDate.getTime())) {
    return makeValidExpiryObj_(parsedDate.getFullYear(), parsedDate.getMonth(), parsedDate.getDate());
  }

  return null;
}

function lookupMonthName_(word) {
  var key = String(word || '').trim().toLowerCase().replace(/\.$/, '');
  return Object.prototype.hasOwnProperty.call(MONTH_LOOKUP, key) ? MONTH_LOOKUP[key] : null;
}

function normalizeYear_(yr) {
  if (yr >= 0 && yr < 100) return 2000 + yr;
  return yr;
}

function makeValidExpiryObj_(y, m, d) {
  if (!isFinite(y) || !isFinite(m) || !isFinite(d)) return null;
  if (y < 1970 || y > 2200) return null;
  if (m < 0 || m > 11) return null;
  var maxDay = new Date(y, m + 1, 0).getDate();
  var safeDay = Math.min(Math.max(1, Math.floor(d)), maxDay);
  return { y: y, m: m, d: safeDay };
}

/**
 * Converts { y, m, d } to a real JS Date (set to noon to prevent DST/timezone drift).
 */
function expiryObjToSheetDate_(expiry) {
  if (!expiry || typeof expiry !== 'object') return '';
  var valid = makeValidExpiryObj_(expiry.y, expiry.m, expiry.d);
  if (!valid) return '';
  return new Date(valid.y, valid.m, valid.d, 12, 0, 0);
}

function getTodayObj_(tz) {
  var now = new Date();
  if (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.formatDate === 'function' && tz) {
    var iso = Utilities.formatDate(now, tz, 'yyyy-MM-dd');
    var p = iso.split('-');
    return { y: Number(p[0]), m: Number(p[1]) - 1, d: Number(p[2]) };
  }
  return { y: now.getFullYear(), m: now.getMonth(), d: now.getDate() };
}

function addDaysObj_(baseObj, days) {
  var dt = new Date(baseObj.y, baseObj.m, baseObj.d, 12, 0, 0);
  dt.setDate(dt.getDate() + days);
  return { y: dt.getFullYear(), m: dt.getMonth(), d: dt.getDate() };
}

function addMonthsObj_(baseObj, months) {
  var totalMonths = baseObj.m + months;
  var targetYear = baseObj.y + Math.floor(totalMonths / 12);
  var targetMonth = ((totalMonths % 12) + 12) % 12;
  return makeValidExpiryObj_(targetYear, targetMonth, baseObj.d);
}

function getDiffDaysFromToday_(expiry, tz) {
  if (!expiry) return null;
  var todayObj = getTodayObj_(tz);
  var t1 = Date.UTC(todayObj.y, todayObj.m, todayObj.d);
  var t2 = Date.UTC(expiry.y, expiry.m, expiry.d);
  return Math.round((t2 - t1) / 86400000);
}

function formatSyncTimestamp_(date, tz) {
  if (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.formatDate === 'function' && tz) {
    return Utilities.formatDate(date, tz, 'MMM d, yyyy h:mm a');
  }
  return MONTH_SHORT_NAMES[date.getMonth()] + ' ' + date.getDate() + ', ' + date.getFullYear();
}

/* ==========================================================================
   SHEET WRITING, STYLING & HISTORY LOG
   ========================================================================== */

/**
 * Writes all mapped cells for an item row without disturbing unmapped columns.
 */
function writeItemRowValues_(sheet, colMap, rowIndex, itemFields) {
  writeCellIfMapped_(sheet, rowIndex, colMap.id, itemFields.id);
  writeCellIfMapped_(sheet, rowIndex, colMap.category, itemFields.categoryName);
  writeCellIfMapped_(sheet, rowIndex, colMap.name, itemFields.name);
  writeCellIfMapped_(sheet, rowIndex, colMap.qty, itemFields.qty);
  writeCellIfMapped_(sheet, rowIndex, colMap.target, itemFields.target);
  writeCellIfMapped_(sheet, rowIndex, colMap.unit, itemFields.unit);
  writeExpiryCell_(sheet, rowIndex, colMap.expiry, itemFields.expiry);
  writeCellIfMapped_(sheet, rowIndex, colMap.location, itemFields.location);
  writeCellIfMapped_(sheet, rowIndex, colMap.notes, itemFields.notes);
  writeCellIfMapped_(sheet, rowIndex, colMap.lastUpdated, itemFields.lastUpdated);
}

/**
 * Safely runs cosmetic styling operations so Google Sheets Table formatting
 * locks never break reads or writes.
 */
function safeStyle_(fn) {
  if (typeof fn !== 'function') return;
  try {
    fn();
  } catch (e) {
    // Ignore cosmetic styling errors on locked/typed Table columns
  }
}

function writeCellIfMapped_(sheet, rowIndex, colIndex, value) {
  if (!colIndex) return;
  sheet.getRange(rowIndex, colIndex).setValue(value);
}

function writeExpiryCell_(sheet, rowIndex, colIndex, expiry) {
  if (!colIndex) return;
  var sheetVal = expiryObjToSheetDate_(expiry);
  sheet.getRange(rowIndex, colIndex).setValue(sheetVal);
}

/**
 * Applies clean visual formatting to a single inventory row on the default sheet:
 * - Category pill background tint + bold category accent text
 * - Soft expiry status tint (red expired, amber <=90d, green fresh)
 * - Clean alignments and subtle ID text styling
 * Note: Never alters column data types or formats so Google Sheets Tables remain intact.
 */
function formatInventoryRow_(sheet, colMap, rowIndex, catObj, expiry, tz) {
  if (colMap.id) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.id)
        .setFontColor('#64748b')
        .setFontSize(9)
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.category && catObj) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.category)
        .setBackground(catObj.soft)
        .setFontColor(catObj.color)
        .setFontWeight('bold')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.name) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.name)
        .setFontWeight('bold')
        .setFontColor('#0f172a')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.qty) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.qty)
        .setHorizontalAlignment('center')
        .setFontWeight('bold')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.target) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.target)
        .setHorizontalAlignment('center')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.unit) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.unit)
        .setHorizontalAlignment('center')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.expiry) {
    safeStyle_(function () {
      var expCell = sheet.getRange(rowIndex, colMap.expiry);
      expCell
        .setHorizontalAlignment('center')
        .setVerticalAlignment('middle');

      var diffDays = getDiffDaysFromToday_(expiry, tz);
      if (diffDays === null) {
        expCell.setBackground(null).setFontColor('#64748b').setFontWeight('normal');
      } else if (diffDays < 0) {
        expCell.setBackground('#fee2e2').setFontColor('#b91c1c').setFontWeight('bold');
      } else if (diffDays <= 90) {
        expCell.setBackground('#fef3c7').setFontColor('#b45309').setFontWeight('bold');
      } else {
        expCell.setBackground('#dcfce7').setFontColor('#15803d').setFontWeight('normal');
      }
    });
  }

  if (colMap.location) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.location).setVerticalAlignment('middle');
    });
  }

  if (colMap.notes) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.notes)
        .setFontColor('#475569')
        .setVerticalAlignment('middle');
    });
  }

  if (colMap.lastUpdated) {
    safeStyle_(function () {
      sheet.getRange(rowIndex, colMap.lastUpdated)
        .setHorizontalAlignment('center')
        .setFontColor('#64748b')
        .setFontSize(9)
        .setVerticalAlignment('middle');
    });
  }
}

/**
 * Refreshes visual formatting across all detected rows in the inventory sheet.
 */
function formatAllInventoryRows_(ss, info, categoryMap) {
  var tz = ss.getSpreadsheetTimeZone();
  var parsed = readSheetInventory_(ss, info);

  for (var i = 0; i < parsed.items.length; i++) {
    var item = parsed.items[i];
    var catObj = categoryMap[item.category] || parsed.categoryMap[item.category] || DEFAULT_CATEGORIES[0];
    formatInventoryRow_(info.sheet, info.colMap, item._rowIndex, catObj, item.expiry, tz);
  }
}

/**
 * Sorts inventory data rows by Category (ascending) and Item Name (ascending)
 * using native Range.sort(), which preserves Google Sheets Tables, typed columns,
 * dropdown chips, and formulas. Stops before any bottom Summary/Total row.
 * Fully isolated in try/catch + flush so locked or protected sheets never fail.
 */
function organizeInventorySheet_(ss, info) {
  if (!info || !info.sheet || !info.colMap) return false;
  var sheet = info.sheet;
  var startRow = info.headerRow + 1;
  var lastRow = sheet.getLastRow();
  var lastCol = sheet.getLastColumn();
  if (lastRow <= startRow || lastCol === 0) return false;

  var sortSpecs = [];
  if (info.colMap.category) {
    sortSpecs.push({ column: info.colMap.category, ascending: true });
  }
  if (info.colMap.name) {
    sortSpecs.push({ column: info.colMap.name, ascending: true });
  }
  if (sortSpecs.length === 0) return false;

  // Determine last non-summary data row (stop before any trailing blank or Total row)
  var scanCount = lastRow - startRow + 1;
  var scanRange = sheet.getRange(startRow, 1, scanCount, lastCol);
  var values = scanRange.getValues();
  var formulas = scanRange.getFormulas();

  var endRow = startRow - 1;
  for (var i = scanCount - 1; i >= 0; i--) {
    var rowVals = values[i];
    var rowFormulas = formulas[i];
    var rawName = info.colMap.name ? String(rowVals[info.colMap.name - 1] || '').trim() : '';
    var firstColVal = String(rowVals[0] || '').trim();
    var hasAnyValue = false;
    for (var c = 0; c < rowVals.length; c++) {
      if (rowVals[c] !== '' && rowVals[c] !== null && rowVals[c] !== undefined) {
        hasAnyValue = true;
        break;
      }
    }
    if (!hasAnyValue) continue;
    if (isSummaryRow_(rawName || firstColVal, rowFormulas, info.colMap)) continue;

    endRow = startRow + i;
    break;
  }

  var numDataRows = endRow - startRow + 1;
  if (numDataRows < 2) return false;

  try {
    sheet.getRange(startRow, 1, numDataRows, lastCol).sort(sortSpecs);
    SpreadsheetApp.flush();
    return true;
  } catch (sortErr) {
    // Gracefully skip if sheet or Table has sorting locked
    return false;
  }
}

/**
 * Ensures the "History Log" tab exists with clean styled headers.
 */
function ensureHistorySheet_(ss) {
  var sheet = ss.getSheetByName(HISTORY_SHEET_NAME);
  if (!sheet) {
    sheet = ss.insertSheet(HISTORY_SHEET_NAME);
  }

  if (sheet.getLastRow() === 0) {
    var headerRange = sheet.getRange(1, 1, 1, HISTORY_HEADERS.length);
    headerRange.setValues([HISTORY_HEADERS]);
    SpreadsheetApp.flush();

    try {
      headerRange
        .setBackground(HEADER_BG_COLOR)
        .setFontColor(HEADER_TEXT_COLOR)
        .setFontWeight('bold')
        .setFontSize(10)
        .setVerticalAlignment('middle');
      sheet.setRowHeight(1, 32);
      sheet.setFrozenRows(1);

      var widths = [160, 125, 195, 220, 95, 95, 85, 130, 260];
      for (var i = 0; i < widths.length; i++) {
        sheet.setColumnWidth(i + 1, widths[i]);
      }
      SpreadsheetApp.flush();
    } catch (styleErr) {
      // Ignore cosmetic styling errors; header values are already committed
    }
  }

  return sheet;
}

/**
 * Appends a single audit trail row to the "History Log" tab.
 */
function appendHistoryLog_(ss, entry) {
  var sheet = ensureHistorySheet_(ss);
  var nextRow = sheet.getLastRow() + 1;
  var expVal = expiryObjToSheetDate_(entry.expiry);

  // Prefix '+' or '=' with a single quote so Google Sheets stores "+5" as plain text
  var changeStr = String(entry.changeText !== undefined ? entry.changeText : '');
  if (/^[+=]/.test(changeStr)) {
    changeStr = "'" + changeStr;
  }

  sheet.getRange(nextRow, 1, 1, HISTORY_HEADERS.length).setValues([[
    entry.timestamp || new Date(),
    entry.action || '',
    entry.categoryName || '',
    entry.itemName || '',
    changeStr,
    entry.newStock !== undefined ? entry.newStock : '',
    entry.unit || '',
    expVal,
    entry.notes || ''
  ]]);
  SpreadsheetApp.flush();

  try {
    sheet.getRange(nextRow, 5).setHorizontalAlignment('center').setFontWeight('bold');
    sheet.getRange(nextRow, 6).setHorizontalAlignment('center');
    sheet.getRange(nextRow, 7).setHorizontalAlignment('center');
    sheet.getRange(nextRow, 8).setHorizontalAlignment('center');

    // Subtle semantic badge tint on Action cell
    var actionLower = String(entry.action || '').toLowerCase();
    var actionCell = sheet.getRange(nextRow, 2).setHorizontalAlignment('center').setFontWeight('bold');
    if (actionLower.indexOf('delete') !== -1) {
      actionCell.setBackground('#fee2e2').setFontColor('#b91c1c');
    } else if (actionLower.indexOf('use') !== -1) {
      actionCell.setBackground('#fef3c7').setFontColor('#b45309');
    } else {
      actionCell.setBackground('#dcfce7').setFontColor('#15803d');
    }
    SpreadsheetApp.flush();
  } catch (styleErr) {
    // Ignore cosmetic styling errors; log row values are already committed
  }
}

/* ==========================================================================
   18 STARTER FAMILY ITEMS (2 ADULTS + 3 KIDS: AGES 9, 5, <1)
   ========================================================================== */

function buildStarterFamilyItems_(tz) {
  var today = getTodayObj_(tz);
  return [
    // 1. Grains & Pasta
    {
      id: 'item-basmati-rice',
      category: 'grains',
      name: 'Basmati Rice',
      qty: 60,
      target: 180,
      unit: 'lbs',
      expiry: addMonthsObj_(today, 18),
      location: 'Main Pantry',
      notes: 'Long-grain, sealed in Mylar bags for family dinners'
    },
    {
      id: 'item-wheat-atta',
      category: 'grains',
      name: 'Whole Wheat Flour (Atta)',
      qty: 45,
      target: 120,
      unit: 'lbs',
      expiry: addMonthsObj_(today, 5),
      location: 'Basement Shelf',
      notes: 'Daily rotis & baking; rotate older bags first'
    },
    {
      id: 'item-rolled-oats',
      category: 'grains',
      name: 'Rolled Oats',
      qty: 24,
      target: 36,
      unit: 'lbs',
      expiry: addMonthsObj_(today, 14),
      location: 'Main Pantry',
      notes: 'Breakfast for kids & soft toddler porridge'
    },

    // 2. Proteins & Lentils
    {
      id: 'item-red-lentils',
      category: 'proteins',
      name: 'Red Lentils (Masoor Daal)',
      qty: 32,
      target: 45,
      unit: 'lbs',
      expiry: addMonthsObj_(today, 22),
      location: 'Main Pantry',
      notes: 'Quick-cooking protein staple for all ages'
    },
    {
      id: 'item-chickpeas',
      category: 'proteins',
      name: 'Canned Chickpeas',
      qty: 48,
      target: 60,
      unit: 'cans',
      expiry: addMonthsObj_(today, 28),
      location: 'Basement Shelf',
      notes: 'Low sodium, ready to heat'
    },
    {
      id: 'item-canned-tuna',
      category: 'proteins',
      name: 'Canned Tuna',
      qty: 10,
      target: 48,
      unit: 'cans',
      expiry: addDaysObj_(today, 38),
      location: 'Kitchen Cabinet',
      notes: 'Use soon in sandwiches & lunches'
    },

    // 3. Canned Goods & Produce
    {
      id: 'item-crushed-tomatoes',
      category: 'canned',
      name: 'Crushed Tomatoes',
      qty: 36,
      target: 48,
      unit: 'cans',
      expiry: addMonthsObj_(today, 20),
      location: 'Basement Shelf',
      notes: 'Base for curries, soups & pasta sauces'
    },
    {
      id: 'item-mixed-veg',
      category: 'canned',
      name: 'Canned Mixed Vegetables',
      qty: 18,
      target: 48,
      unit: 'cans',
      expiry: addDaysObj_(today, 64),
      location: 'Main Pantry',
      notes: 'Peas, carrots & sweet corn'
    },

    // 4. Baby & Kids (<1 to 3 yrs)
    {
      id: 'item-infant-formula',
      category: 'baby',
      name: 'Infant Formula (Stage 1–2)',
      qty: 6,
      target: 18,
      unit: 'cans',
      expiry: addDaysObj_(today, 52),
      location: 'Kitchen Cabinet',
      notes: 'Year 1 baby supply; transitions to milk powder next year'
    },
    {
      id: 'item-toddler-pouches',
      category: 'baby',
      name: 'Toddler Fruit & Grain Pouches',
      qty: 12,
      target: 60,
      unit: 'pouches',
      expiry: addDaysObj_(today, -14),
      location: 'Main Pantry',
      notes: 'Replace expired batch with fresh toddler snacks'
    },
    {
      id: 'item-powdered-milk',
      category: 'baby',
      name: 'Powdered Whole Milk',
      qty: 14,
      target: 30,
      unit: 'lbs',
      expiry: addMonthsObj_(today, 16),
      location: 'Basement Shelf',
      notes: 'For all 3 kids as baby grows from age 1 to 3'
    },

    // 5. Water & Drinks
    {
      id: 'item-bottled-water',
      category: 'water',
      name: 'Bottled Drinking Water',
      qty: 65,
      target: 90,
      unit: 'gallons',
      expiry: addMonthsObj_(today, 15),
      location: 'Garage Bins',
      notes: 'Stored off concrete floor, shaded corner'
    },
    {
      id: 'item-water-filters',
      category: 'water',
      name: 'Water Filter Cartridges',
      qty: 4,
      target: 6,
      unit: 'packs',
      expiry: null,
      location: 'Closet / Hallway',
      notes: 'Gravity filter replacements for 3-year clean water'
    },

    // 6. Oils, Spices & Cooking
    {
      id: 'item-cooking-oil',
      category: 'oils',
      name: 'Olive & Cooking Oil',
      qty: 5,
      target: 12,
      unit: 'bottles',
      expiry: addDaysObj_(today, 76),
      location: 'Main Pantry',
      notes: 'Keep cool & dark; rotate oldest bottle to kitchen'
    },
    {
      id: 'item-desi-ghee',
      category: 'oils',
      name: 'Desi Ghee',
      qty: 8,
      target: 10,
      unit: 'jars',
      expiry: addMonthsObj_(today, 19),
      location: 'Kitchen Cabinet',
      notes: 'Shelf-stable cooking fat & toddler calorie booster'
    },
    {
      id: 'item-sea-salt',
      category: 'oils',
      name: 'Sea Salt',
      qty: 10,
      target: 12,
      unit: 'lbs',
      expiry: null,
      location: 'Main Pantry',
      notes: 'Iodized & pink salt, keeps indefinitely'
    },

    // 7. Essentials & First Aid
    {
      id: 'item-diapers-wipes',
      category: 'essentials',
      name: 'Diapers & Baby Wipes',
      qty: 5,
      target: 16,
      unit: 'packs',
      expiry: null,
      location: 'Closet / Hallway',
      notes: 'Sizes 3–5 for Years 1–2 before potty training'
    },
    {
      id: 'item-first-aid',
      category: 'essentials',
      name: "Children's Fever & First Aid Kit",
      qty: 1,
      target: 2,
      unit: 'boxes',
      expiry: addDaysObj_(today, -9),
      location: 'Kitchen Cabinet',
      notes: 'Check children’s fever meds & restock bandages'
    }
  ];
}

/* ==========================================================================
   GENERAL HELPERS
   ========================================================================== */

function ensureFormSubmitTrigger_(ss) {
  var triggers = ScriptApp.getProjectTriggers();
  for (var i = 0; i < triggers.length; i++) {
    if (
      triggers[i].getHandlerFunction() === 'onFormSubmit' &&
      triggers[i].getEventType() === ScriptApp.EventType.ON_FORM_SUBMIT
    ) {
      return;
    }
  }
  ScriptApp.newTrigger('onFormSubmit')
    .forSpreadsheet(ss)
    .onFormSubmit()
    .create();
}

function getNamedFormValue_(namedValues, candidateKeys) {
  if (!namedValues) return '';
  var lowerMap = {};
  var keys = Object.keys(namedValues);
  for (var i = 0; i < keys.length; i++) {
    lowerMap[keys[i].trim().toLowerCase()] = namedValues[keys[i]];
  }
  for (var k = 0; k < candidateKeys.length; k++) {
    var val = lowerMap[candidateKeys[k].toLowerCase()];
    if (Array.isArray(val) && val.length > 0 && String(val[0] || '').trim() !== '') {
      return String(val[0]).trim();
    }
    if (typeof val === 'string' && val.trim() !== '') {
      return val.trim();
    }
  }
  return '';
}

function findItemById_(items, id) {
  for (var i = 0; i < items.length; i++) {
    if (items[i].id === id) return items[i];
  }
  return null;
}

function findItemByName_(items, normalizedName, preferredCategoryId) {
  var fallback = null;
  for (var i = 0; i < items.length; i++) {
    if (normalizeKey_(items[i].name) === normalizedName) {
      if (preferredCategoryId && items[i].category === preferredCategoryId) {
        return items[i];
      }
      if (!fallback) fallback = items[i];
    }
  }
  return fallback;
}

function generateUniqueItemId_(name, existingItems) {
  var used = {};
  for (var i = 0; i < existingItems.length; i++) {
    used[existingItems[i].id] = true;
  }
  var base = 'item-' + (slugify_(name).slice(0, 28) || 'supply');
  var shortRand = (typeof Utilities !== 'undefined' && Utilities && typeof Utilities.getUuid === 'function')
    ? Utilities.getUuid().slice(0, 6)
    : Math.random().toString(36).slice(2, 8);
  var candidate = base + '-' + shortRand;
  while (used[candidate]) {
    candidate = base + '-' + Math.random().toString(36).slice(2, 8);
  }
  return candidate;
}

function defaultTargetForCategory_(catId) {
  if (catId === 'grains') return 60;
  if (catId === 'proteins') return 40;
  if (catId === 'baby') return 24;
  if (catId === 'water') return 40;
  return 20;
}

function normalizeHeader_(val) {
  return String(val === null || val === undefined ? '' : val)
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

function normalizeKey_(val) {
  return String(val === null || val === undefined ? '' : val)
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

function slugify_(text) {
  return String(text || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

function parseQuantity_(val) {
  if (typeof val === 'number') {
    return isFinite(val) ? Math.max(0, roundQty_(val)) : 0;
  }
  var str = String(val === null || val === undefined ? '' : val).replace(/,/g, '').trim();
  if (!str) return 0;
  var match = str.match(/^-?\d+(?:\.\d+)?/);
  if (!match) return 0;
  var n = Number(match[0]);
  return isFinite(n) ? Math.max(0, roundQty_(n)) : 0;
}

function extractUnitFromQuantityString_(val) {
  if (typeof val !== 'string') return '';
  var match = val.trim().match(/^-?\d[\d,]*(?:\.\d+)?\s*([a-zA-Z]+)$/);
  return match ? match[1].toLowerCase() : '';
}

function parseSignedNumber_(val) {
  if (typeof val === 'number') {
    return isFinite(val) ? roundQty_(val) : null;
  }
  var str = String(val === null || val === undefined ? '' : val).replace(/,/g, '').trim();
  if (!str) return null;
  var n = Number(str);
  return isFinite(n) ? roundQty_(n) : null;
}

function parseOptionalNonNegativeNumber_(val) {
  if (val === null || val === undefined || val === '') return undefined;
  if (typeof val === 'number') {
    return isFinite(val) ? Math.max(0, roundQty_(val)) : undefined;
  }
  var str = String(val).replace(/,/g, '').trim();
  if (!str) return undefined;
  var match = str.match(/^-?\d+(?:\.\d+)?/);
  if (!match) return undefined;
  var n = Number(match[0]);
  return isFinite(n) ? Math.max(0, roundQty_(n)) : undefined;
}

function roundQty_(n) {
  return Math.round(n * 100) / 100;
}

function getErrorMessage_(err) {
  if (!err) return 'Unexpected error occurred.';
  if (typeof err === 'string') return err;
  return err.message || String(err);
}

function releaseLockSafe_(lock) {
  if (!lock) return;
  try {
    lock.releaseLock();
  } catch (e) {
    // Ignore release errors when lock was not held
  }
}

function tryShowAlert_(title, message) {
  try {
    SpreadsheetApp.getUi().alert(title, message, SpreadsheetApp.getUi().ButtonSet.OK);
  } catch (e) {
    // Running from Web App or trigger where Spreadsheet UI is unavailable
  }
}
