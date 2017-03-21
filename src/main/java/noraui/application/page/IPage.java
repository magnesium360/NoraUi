package noraui.application.page;

import noraui.application.page.Page.PageElement;
import noraui.exception.Callbacks.Callback;

public interface IPage {

    /**
     * @return noraui.exception.Callbacks.Callback of page.
     */
    Callback getCallBack();

    /**
     * @param key
     *            of PageElement
     * @return noraui.application.page.Page.PageElement
     */
    PageElement getPageElementByKey(String key);

    /**
     * @return key of page
     */
    String getPageKey();

    /**
     * @return application name
     */
    String getApplication();

    /**
     * @return get the linked container page.
     */
    Page getMotherPage();

    /**
     * set the linked container page.
     *
     * @param motherPage
     *            is the mother page linked
     */
    void setMotherPage(Page motherPage);

    /**
     * @param elements
     *            in a array of element
     * @return true or false
     */
    boolean checkPage(Object... elements);

    interface IPageElement {

        /**
         * @return key of page element
         */
        String getKey();

        /**
         * @return label of page element
         */
        String getLabel();

        /**
         * @return page of page element
         */
        IPage getPage();

    }
}