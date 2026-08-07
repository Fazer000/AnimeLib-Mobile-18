/**
 * Иньекция кнопки "Скачанное" после элемента "Загрузки" в меню сайта.
 */
(function() {
    function injectDownloadedMenuItem() {
        if (document.getElementById('app-downloaded-anime-item')) {
            return;
        }

        var menuItems = document.querySelectorAll('.menu-item');
        var downloadsEl = null;

        for (var i = 0; i < menuItems.length; i++) {
            var item = menuItems[i];
            var href = item.getAttribute('href') || '';
            var text = (item.textContent || '').trim();

            if (href.indexOf('/downloads') !== -1 || text.indexOf('Загрузки') !== -1) {
                downloadsEl = item;
                break;
            }
        }

        if (!downloadsEl) {
            return;
        }

        var downloadedItem = downloadsEl.cloneNode(true);
        downloadedItem.id = 'app-downloaded-anime-item';
        downloadedItem.setAttribute('href', 'javascript:void(0);');

        var textEl = downloadedItem.querySelector('.menu-item__text');
        if (textEl) {
            textEl.textContent = 'Скачанное';
        } else {
            downloadedItem.textContent = 'Скачанное';
        }

        downloadedItem.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            if (e.stopImmediatePropagation) {
                e.stopImmediatePropagation();
            }
            if (window.AndroidInterface && window.AndroidInterface.onDownloadedButtonClicked) {
                console.log('[AnimeLIB] "Скачанное" menu item clicked');
                window.AndroidInterface.onDownloadedButtonClicked();
            } else {
                console.error('[AnimeLIB] AndroidInterface.onDownloadedButtonClicked not found');
            }
        }, true);

        if (downloadsEl.parentNode) {
            downloadsEl.parentNode.insertBefore(downloadedItem, downloadsEl.nextSibling);
            console.log('[AnimeLIB] Successfully injected "Скачанное" menu item');
        }
    }

    injectDownloadedMenuItem();

    if (!window.animelibDownloadedObserverSetup) {
        window.animelibDownloadedObserverSetup = true;

        var observer = new MutationObserver(function() {
            injectDownloadedMenuItem();
        });

        if (document.body) {
            observer.observe(document.body, { childList: true, subtree: true });
        } else {
            document.addEventListener('DOMContentLoaded', function() {
                if (document.body) {
                    observer.observe(document.body, { childList: true, subtree: true });
                }
            });
        }

        setInterval(injectDownloadedMenuItem, 1500);
    }
})();
