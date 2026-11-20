import { useParams } from "@tanstack/react-router";
import { NotFoundPage } from "@/pages/NotFoundPage.tsx";
import { ServerSettingsHeader } from "@/components/servers/settings/ServerSettingsHeader.tsx";
import { CategoryButtonList } from "@/components/servers/settings/CategoryButtonList.tsx";
import { ServerSettingsCategory } from "@/components/servers/settings/ServerSettingsCategory.tsx";
import { type ReactNode, useMemo } from "react";
import { Spinner } from "@/components/ui/spinner.tsx";
import { Form } from "@/components/ui/form.tsx";
import { Input, InputIcon, InputRoot } from "@/components/ui/input.tsx";
import {
    CircleX,
    FileInput,
    Paintbrush,
    RefreshCw,
    Search,
} from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { ImportSettingsDialog } from "@/components/servers/dialogs/ImportSettingsDialog.tsx";
import { useMaxPlayers } from "@/hooks/useMaxPlayers.ts";
import { SyncSettingsDialog } from "@/components/servers/dialogs/SyncSettingsDialog.tsx";
import { dialog } from "@/store/dialogStore.tsx";
import { ResetServerSettingsDialog } from "@/components/servers/dialogs/ResetServerSettingsDialog.tsx";
import { CustomIniEditor } from "@/components/servers/settings/CustomIniEditor.tsx";
import { useServerSettingsController } from "@/hooks/serverSettings/useServerSettingsController";
import { CUSTOM_CATEGORY } from "@/lib/serverSettings";
import { ServerDirectoryChangeOverlay } from "@/components/servers/ServerDirectoryChangeOverlay.tsx";
import { ConfirmDialog } from "@/components/custom/ConfirmDialog.tsx";
import { wipeSavegames } from "@/api/serverApi.ts";
import { toast } from "sonner";

export function ServerSettingsPage() {
    const params = useParams({ strict: false });
    const controller = useServerSettingsController(params.serverId);

    const {
        server,
        profile,
        templates,
        form,
        processedTemplates,
        categoriesList,
        customIni,
        syncableSettings,
        categoryRefs,
        registerCategoryRef,
        containerRef,
        search,
        status,
        processingState,
        loadingState,
        actions,
    } = controller;

    const maxPlayers = useMaxPlayers(server, profile);
    const {
        game: customGameIni,
        gus: customGusIni,
        setGame,
        setGus,
    } = customIni;

    const categoryElements = useMemo(() => {
        if (!server || !templates || !profile) return null;
        return Object.entries(processedTemplates)
            .filter(([category]) => category !== CUSTOM_CATEGORY)
            .map(([category, templateGroup], index) => (
                <ServerSettingsCategory
                    key={category}
                    category={category}
                    templates={templateGroup}
                    form={form}
                    index={index}
                    categoryRef={(el) => registerCategoryRef(category, el)}
                />
            ));
    }, [
        processedTemplates,
        form,
        registerCategoryRef,
        server,
        templates,
        profile,
    ]);

    const customCategoryElement = useMemo(
        (): ReactNode => (
            <div
                id={CUSTOM_CATEGORY}
                ref={(el) => registerCategoryRef(CUSTOM_CATEGORY, el)}
                className="grid bg-secondary rounded-md p-5 mt-5 grid-cols-1 xl:grid-cols-2 2xl:grid-cols-3 gap-5">
                <h2 className="uppercase xl:col-span-2 2xl:col-span-3">
                    {CUSTOM_CATEGORY}
                </h2>
                <div className="xl:col-span-2 2xl:col-span-3">
                    <CustomIniEditor
                        title="GameUserSettings.ini"
                        location="GameUserSettings.ini"
                        sections={customGusIni}
                        onChange={setGus}
                        form={form}
                        className="w-full"
                    />
                </div>
                <div className="xl:col-span-2 2xl:col-span-3">
                    <CustomIniEditor
                        title="Game.ini"
                        location="Game.ini"
                        sections={customGameIni}
                        onChange={setGame}
                        form={form}
                        className="w-full"
                    />
                </div>
            </div>
        ),
        [
            customGusIni,
            customGameIni,
            setGus,
            setGame,
            form,
            registerCategoryRef,
        ],
    );

    const formElement = useMemo(
        () => (
            <Form {...form}>
                {categoryElements}
                {customCategoryElement}
            </Form>
        ),
        [form, categoryElements, customCategoryElement],
    );

    if (
        status.isServerLoading ||
        status.isProfileLoading ||
        status.isTemplateLoading ||
        !profile ||
        !templates
    ) {
        return <Spinner size="large" />;
    }

    if (!server) {
        return <NotFoundPage />;
    }

    return (
        <ServerDirectoryChangeOverlay server={server}>
            <ServerSettingsHeader
                server={server}
                maxPlayers={maxPlayers}
                isLoading={loadingState.isLoading}
                isProfileLoading={status.isProfileLoading}
                onSave={form.handleSubmit(actions.handleSubmit)}
                hasChanges={form.formState.isDirty}
            />
            <div className="flex grow mt-5 h-0">
                <div className="w-44 min-w-fit 2xl:w-56 mr-4 sm:flex hidden flex-col">
                    <div
                        ref={search.searchContainerRef}
                        className="relative my-3">
                        <InputRoot>
                            <InputIcon>
                                <Search className="text-muted-foreground" />
                            </InputIcon>
                            <Input
                                placeholder="Search Setting"
                                type="search"
                                onChange={(event) =>
                                    search.onInputChange(event.target.value)
                                }
                                onFocus={search.showResults}
                                onClick={search.showResults}
                            />
                        </InputRoot>
                        {search.isVisible && (
                            <ul className="absolute z-50 mt-1 w-full max-h-60 overflow-auto rounded-md border bg-popover text-popover-foreground shadow-md">
                                {search.results.map((res) => (
                                    <li
                                        key={res.id}
                                        className="cursor-pointer px-3 py-2 hover:bg-accent hover:text-accent-foreground text-sm flex items-center justify-between"
                                        onMouseDown={(event) => {
                                            event.preventDefault();
                                            search.onResultSelect(res);
                                        }}>
                                        <span className="truncate mr-2">
                                            {res.name}
                                        </span>
                                        <span className="text-muted-foreground text-xs">
                                            {res.category}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    <div className="grow flex flex-col gap-2 overflow-y-auto pr-2">
                        <CategoryButtonList
                            categories={categoriesList}
                            containerRef={containerRef}
                            categoryRefs={categoryRefs}
                        />
                    </div>
                </div>
                <div className="grow flex flex-col min-w-0">
                    <div>
                        <div className="overflow-x-auto min-w-0 w-full">
                            <div className="flex w-max gap-3 py-3 mx-auto sm:ml-auto sm:mr-0">
                                <Button
                                    onClick={() =>
                                        dialog(
                                            <SyncSettingsDialog
                                                server={server}
                                                settings={syncableSettings}
                                                setLoading={
                                                    loadingState.setLoading
                                                }
                                            />,
                                        )
                                    }
                                    variant="input"
                                    disabled={loadingState.isLoading}>
                                    {loadingState.isLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        <RefreshCw />
                                    )}
                                    Sync
                                </Button>
                                <Button
                                    onClick={() =>
                                        dialog(
                                            <ConfirmDialog
                                                title="Wipe Save Games"
                                                message="Are you sure you want to wipe all SaveGames? This action cannot be undone."
                                                confirmText="Wipe Savegames"
                                                onConfirm={() => {
                                                    loadingState.setLoading(
                                                        true,
                                                    );
                                                    wipeSavegames(server.id)
                                                        .then(() => {
                                                            toast.success(
                                                                "Save games wiped successfully.",
                                                            );
                                                        })
                                                        .finally(() =>
                                                            loadingState.setLoading(
                                                                false,
                                                            ),
                                                        );
                                                }}
                                            />,
                                        )
                                    }
                                    variant="input"
                                    disabled={
                                        loadingState.isLoading ||
                                        server.state != "OFFLINE"
                                    }>
                                    {loadingState.isLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        <CircleX />
                                    )}
                                    Wipe Save Games
                                </Button>
                                <Button
                                    onClick={() =>
                                        dialog(
                                            <ResetServerSettingsDialog
                                                server={server}
                                                profile={profile}
                                            />,
                                        )
                                    }
                                    variant="input"
                                    disabled={loadingState.isLoading}>
                                    {loadingState.isLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        <Paintbrush />
                                    )}
                                    Reset
                                </Button>
                                <Button
                                    onClick={() =>
                                        dialog(
                                            <ImportSettingsDialog
                                                profile={profile}
                                                setLoading={
                                                    loadingState.setLoading
                                                }
                                            />,
                                        )
                                    }
                                    variant="input"
                                    disabled={loadingState.isLoading}>
                                    {loadingState.isLoading ? (
                                        <Spinner size="tiny" />
                                    ) : (
                                        <FileInput />
                                    )}
                                    Import
                                </Button>
                            </div>
                        </div>
                    </div>
                    <div
                        id="setting-categories"
                        ref={containerRef}
                        className="grow overflow-y-auto scroll-smooth pr-2 min-w-0">
                        {processingState.isProcessing ||
                        processingState.isDeferredPending ? (
                            <div className="flex items-center justify-center h-full">
                                <Spinner size="large" />
                            </div>
                        ) : (
                            formElement
                        )}
                    </div>
                </div>
            </div>
        </ServerDirectoryChangeOverlay>
    );
}
